package ca.disjoint.fitcustomizer;

import com.garmin.fit.Fit;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.IVersionProvider;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;

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
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import ca.disjoint.fitcustomizer.GarminActivityLoader;
import ca.disjoint.fitcustomizer.FitWriter;
import ca.disjoint.fitcustomizer.GarminSwimActivity;

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
            "--randomize-ctime" }, negatable = true, description = "Randomize the activity start time. This allows you to upload duplicate activities to Strava, Garmin Connect, and other similar services (default: ${DEFAULT-VALUE})")
    private boolean randomizeCreationTime = true;

    private static final Logger LOGGER = LogManager.getLogger(SwimEditor.class);

    private Terminal terminal;

    public Integer call() {
        if (verbose) {
            Configurator.setRootLevel(Level.DEBUG);
            LOGGER.log(Level.INFO, "Verbose logging enabled");
        }

        try {
            float poolLength = 0f;

            terminal = TerminalBuilder.builder().system(true).signalHandler(Terminal.SignalHandler.SIG_IGN).build();

            GarminSwimActivity activity = new GarminSwimActivity();
            GarminActivityLoader gal = new GarminActivityLoader(swimmingFitFile, activity);

            if (randomizeCreationTime) {
                activity.randomizeCreationTime();
            }

            if (editMode) {
                poolLength = readPoolLength();
                LOGGER.log(Level.DEBUG, "User entered pool length: " + poolLength);
                activity.updateSwimmingPoolLength(poolLength);
                terminal.puts(Capability.clear_screen);
                terminal.flush();
            }

            System.out.println(activity.getActivitySummary());
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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SwimEditor()).execute(args);
        System.exit(exitCode);
    }

    private float readPoolLength() {
        LineReader reader = LineReaderBuilder.builder().build();

        String prompt = new AttributedStringBuilder().style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("Pool length (meters):").style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(System.lineSeparator() + "value> ").toAnsi();

        String userInput = "";
        String promptRightSide = null;
        float currentPoolLength = 15.24f;
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
                poolLength = Float.parseFloat(userInput);

            } catch (UserInterruptException e) {
                throw new RuntimeException("");
            } catch (EndOfFileException e) {
                throw new RuntimeException("");
            } catch (NumberFormatException e) {
                promptRightSide = new AttributedStringBuilder()
                        .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED)).append("ERROR: ")
                        .style(AttributedStyle.BOLD).append("\"" + userInput + "\" is not a valid pool length")
                        .toAnsi();
            }
        }

        return poolLength;
    }

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
