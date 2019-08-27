package ca.disjoint.fitcustomizer;

import com.garmin.fit.Fit;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.SwimStroke;
import com.garmin.fit.LengthType;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.IVersionProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.EnumCompleter;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

@Command(name = "SwimEditor.jar", mixinStandardHelpOptions = true, versionProvider = SwimEditor.PropertiesVersionProvider.class, description = "Edit Garmin swim .fit files to add heartrate data, correct strokes, and more.")
public class SwimEditor implements Callable<Integer> {
    @Option(names = { "-v",
            "--verbose" }, description = "Verbose mode. Helpful for troubleshooting. Logs are available in the \"application.log\" file in the current directory. (default: ${DEFAULT-VALUE})")
    private boolean verbose = false;

    @Parameters(arity = "1", paramLabel = "FILE", description = "Swimming FIT file to process.")
    private File swimmingFitFile;

    @Option(names = { "-e",
            "--edit" }, description = "Interactively edit the pool length, stroke types, and other attributes. (default: ${DEFAULT-VALUE})")
    private boolean editMode = false;

    @Option(names = {
            "--no-randomize-ctime" }, negatable = true, description = "Randomize the activity start time. This allows you to upload duplicate activities to Strava, Garmin Connect, and other similar services (default: ${DEFAULT-VALUE})")
    private boolean randomizeCreationTime = true;

    private static final Logger LOGGER = LogManager.getLogger(SwimEditor.class);

    private Terminal terminal;
    private final InputStream input;
    private final OutputStream output;
    private final String[] cliArgs;
    private GarminSwimActivity garminSwimActivity;
    private LineReader reader;

    public SwimEditor(final InputStream input, final OutputStream output, final Terminal terminal,
            final String[] args) {
        this.input = input;
        this.output = output;
        this.cliArgs = args;
        this.terminal = terminal;
        reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    @SuppressWarnings("checkstyle:DesignForExtension")
    public Integer call() {
        if (verbose) {
            Configurator.setRootLevel(Level.DEBUG);
            LOGGER.log(Level.INFO, "Verbose logging enabled");
        }

        try {
            float poolLength = 0f;
            String updatedFitFileName = "";
            garminSwimActivity = new GarminSwimActivity();
            GarminActivityLoader gal = new GarminActivityLoader(swimmingFitFile, garminSwimActivity);

            if (randomizeCreationTime) {
                garminSwimActivity.randomizeCreationTime();
            }

            if (editMode) {
                // Allow the user to edit the pool length
                poolLength = readPoolLength(garminSwimActivity.getPoolLength());
                LOGGER.log(Level.DEBUG, "User entered pool length: " + poolLength);
                garminSwimActivity.updateSwimmingPoolLength(poolLength);
                terminal.puts(Capability.clear_screen);
                terminal.flush();

                // Allow the user to edit the individual laps/strokes
                editSwimLaps();

                // Generate the newly updated FIT file
                FitWriter fr = new FitWriter(garminSwimActivity, swimmingFitFile.getName());
                updatedFitFileName = fr.writeFitFile();
            }

            terminal.writer().append(garminSwimActivity.getActivitySummaryHeader());
            terminal.writer().append(garminSwimActivity.getActivitySummary());

            if (editMode) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                asb.append(String.format("Editing complete - new FIT file available at: "));
                asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
                asb.append(String.format(updatedFitFileName));
                asb.append(System.lineSeparator());
                terminal.writer().append(asb.toAnsi());
            }

            terminal.flush();
        } catch (Exception ex) {
            String exceptionMsg = ex.getMessage();
            String msg = String.format("Error: %s", ex.getMessage());
            if (exceptionMsg != "") {
                System.err.println(msg);
            }
            LOGGER.log(Level.ERROR, msg);

            if (verbose) {
                ex.printStackTrace();
            }
            return 1;
        }

        return 0;
    }

    public static void main(final String[] args) throws IOException {
        Terminal term = TerminalBuilder.builder().system(true).signalHandler(Terminal.SignalHandler.SIG_IGN).build();

        SwimEditor swm = new SwimEditor(System.in, System.out, term, args);
        int exitCode = swm.start();
        System.exit(exitCode);
    }

    @SuppressWarnings("checkstyle:DesignForExtension")
    public int start() {
        return new CommandLine(this).execute(cliArgs);
    }

    private void editSwimLaps() {
        while (true) {
            int inputLapNumberToEdit = readSwimLapNumber();
            if (inputLapNumberToEdit == 0) {
                terminal.puts(Capability.clear_screen);
                terminal.flush();
                break;
            }

            LOGGER.log(Level.DEBUG, "User would like to edit swim lap: " + inputLapNumberToEdit);

            terminal.puts(Capability.clear_screen);
            terminal.writer().append(garminSwimActivity.getLapSummary(inputLapNumberToEdit - 1));
            terminal.writer().append(System.lineSeparator());
            terminal.writer().append(System.lineSeparator());
            terminal.flush();
            editSwimLap(inputLapNumberToEdit - 1);
            garminSwimActivity.recalculateActivityStats();
        }
    }

    private void editSwimLap(final int lapNumber) {
        GarminLap lap = garminSwimActivity.getGarminLap(lapNumber);
        List<LengthMesg> lengths = lap.getLengthMessages();
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal)
                .completer(new EnumCompleter(SwimStroke.class)).build();
        String promptRightSide = null;

        for (int i = 0; i < lengths.size(); i++) {
            LengthMesg length = lengths.get(i);
            SwimStroke lengthStroke = SwimStroke.INVALID;
            if (length.getLengthType() == LengthType.ACTIVE) {
                lengthStroke = length.getSwimStroke();
            }

            float lengthAvgSpeed = 0f;
            if (length.getAvgSpeed() != null) {
                lengthAvgSpeed = Utils.PACE_PER_HUNDRED_METERS / length.getAvgSpeed();
            }

            String prompt = new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append("Length ")
                    .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                    .append(String.format("%s/%s ", i + 1, lengths.size()))
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                    .append(String.format("(time %s, pace %s/100m) ",
                            Utils.convertFloatToStringDate(length.getTotalElapsedTime()),
                            Utils.convertFloatToStringDate(lengthAvgSpeed)))
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).append("(Tab to complete):")
                    .append(System.lineSeparator() + "stroke> ").toAnsi();
            String userInput = "";

            String line = null;
            try {
                line = lineReader.readLine(prompt, promptRightSide, (MaskingCallback) null,
                        lengthStroke.toString().toLowerCase());
                ParsedLine pl = lineReader.getParser().parse(line, 0);
                userInput = pl.words().get(0);
                LOGGER.log(Level.DEBUG, "Raw user-entered stroke: " + userInput);
                SwimStroke inputStroke = SwimStroke.valueOf(userInput.toUpperCase());
                promptRightSide = null;
                LOGGER.log(Level.DEBUG, "Determined entered stroke to be: " + inputStroke);

                // Update the swim stroke to be the value entered in by the user
                length.setSwimStroke(inputStroke);
                length.setLengthType(LengthType.ACTIVE);
            } catch (UserInterruptException e) {
                throw new RuntimeException("");
            } catch (EndOfFileException e) {
                throw new RuntimeException("");
            } catch (IllegalArgumentException e) {
                String err = "Invalid stroke \"" + userInput + "\"";
                LOGGER.log(Level.DEBUG, "Error: " + err);
                promptRightSide = new AttributedStringBuilder()
                        .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED)).append("ERROR: ")
                        .style(AttributedStyle.BOLD).append(err).toAnsi();
                i--;
            }
        }

        // Finally replace the model with this newly updated lap (probably un-necessary?)
        garminSwimActivity.replaceGarminLap(lapNumber, lap);
    }

    private int readSwimLapNumber() {
        LOGGER.log(Level.DEBUG, "Presenting prompt asking user to enter a swim lap number to edit");
        String prompt = new AttributedStringBuilder().style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("Which Lap would you like to edit? (Ctrl+d to finish):")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(System.lineSeparator() + "lap number> ").toAnsi();
        String userInput = "";
        String promptRightSide = null;
        int lapNumber = -1;
        boolean isLapNumberValid = false;

        while (!isLapNumberValid) {
            String line = null;
            try {
                terminal.puts(Capability.clear_screen);
                terminal.writer().append(garminSwimActivity.getLapSummary());
                terminal.flush();
                line = reader.readLine(prompt, promptRightSide, (MaskingCallback) null, null);
                ParsedLine pl = reader.getParser().parse(line, 0);
                userInput = pl.words().get(0);
                LOGGER.log(Level.DEBUG, "Raw user-entered lap number: " + userInput);
                lapNumber = Integer.parseInt(userInput);

                if (lapNumber == -1) {
                    // primarily useful for testing
                    LOGGER.log(Level.DEBUG, "Simulated ctrl+d invoked");
                    throw new EndOfFileException();
                }

                isLapNumberValid = validateLapNumberInput(lapNumber);
                if (!isLapNumberValid) {
                    LOGGER.log(Level.DEBUG, "Lap " + userInput + " cannot be edited");
                    throw new NumberFormatException();
                }
            } catch (UserInterruptException e) {
                throw new RuntimeException("");
            } catch (EndOfFileException e) {
                LOGGER.log(Level.DEBUG, "Finished entering in swim laps (Ctrl + d)");
                lapNumber = 0;
                isLapNumberValid = true;
            } catch (NumberFormatException e) {
                String err = "\"" + userInput + "\" is not a valid lap number";
                LOGGER.log(Level.DEBUG, "Error: " + err);
                promptRightSide = new AttributedStringBuilder()
                        .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED)).append("ERROR: ")
                        .style(AttributedStyle.BOLD).append(err).toAnsi();
                isLapNumberValid = false;
            }
        }

        return lapNumber;
    }

    private boolean validateLapNumberInput(final int input) {
        boolean result = false;
        LOGGER.log(Level.DEBUG, "Validating whether " + input + " is in the active laps list");
        List<Integer> activeLaps = garminSwimActivity.getActiveSwimLaps();
        result = activeLaps.contains(Integer.valueOf(input - 1));
        LOGGER.log(Level.DEBUG, "Validation result: " + result);
        return result;
    }

    private float readPoolLength(final float currentPoolLength) {
        LOGGER.log(Level.DEBUG, "Presenting prompt to read swimming pool length");

        String prompt = new AttributedStringBuilder().style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("Pool length (meters):").style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(System.lineSeparator() + "value> ").toAnsi();

        String userInput = "";
        String promptRightSide = null;
        float poolLength = 0f;

        while (poolLength <= 0f) {
            String line = null;
            try {
                terminal.puts(Capability.clear_screen);
                terminal.flush();
                line = reader.readLine(prompt, promptRightSide, (MaskingCallback) null,
                        Float.toString(currentPoolLength));

                ParsedLine pl = reader.getParser().parse(line, 0);
                userInput = pl.words().get(0);
                LOGGER.log(Level.DEBUG, "Raw user-entered pool length: " + userInput);
                poolLength = Float.parseFloat(userInput);

            } catch (UserInterruptException e) {
                throw new RuntimeException("");
            } catch (EndOfFileException e) {
                throw new RuntimeException("");
            } catch (NumberFormatException e) {
                String err = "\"" + userInput + "\" is not a valid pool length";
                LOGGER.log(Level.DEBUG, "Error: " + err);
                promptRightSide = new AttributedStringBuilder()
                        .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED)).append("ERROR: ")
                        .style(AttributedStyle.BOLD).append(err).toAnsi();
            }
        }

        return poolLength;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    static class PropertiesVersionProvider implements IVersionProvider {
        public String[] getVersion() throws Exception {
            URL url = getClass().getResource("/META-INF/application.properties");
            if (url == null) {
                return new String[] { "N/A" };
            }
            Properties properties = new Properties();
            properties.load(url.openStream());
            return new String[] { "SwimEditor.jar v" + properties.getProperty("application.version"),
                    String.format("FIT protocol %d.%d, profile %.2f %s", Fit.PROTOCOL_VERSION_MAJOR,
                            Fit.PROTOCOL_VERSION_MINOR, Fit.PROFILE_VERSION / 100.0, Fit.PROFILE_TYPE) };
        }
    }
}
