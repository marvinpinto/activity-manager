/*
    Activity Manager
    Copyright (C) 2019 - Marvin Pinto

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ca.disjoint.fit;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.SwimStroke;
import com.garmin.fit.LengthType;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.DateTime;

import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import org.jline.terminal.Terminal;
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

@Command(name = "swim", mixinStandardHelpOptions = true, description = "View pool swimming lap info, edit strokes, add heartrate data, and more.", usageHelpAutoWidth = true)
public class SwimEditor implements Callable<Integer> {
    @Mixin
    private ReusableOptions reusableOptionsMixin;

    @Parameters(arity = "1", paramLabel = "FILE", description = "Pool swimming FIT file to process.")
    private File swimmingFitFile;

    @Option(names = { "-e",
            "--edit" }, description = "Interactively edit the pool length, stroke types, and other attributes. (default: ${DEFAULT-VALUE})")
    private boolean editMode = false;

    @Option(names = {
            "--no-randomize-ctime" }, negatable = true, description = "Randomize the activity start time. This allows you to upload duplicate activities to Strava, Garmin Connect, and other similar services (default: ${DEFAULT-VALUE})")
    private boolean randomizeCreationTime = true;

    @Option(names = "--hr-data", arity = "0..1", paramLabel = "FILE", description = "FIT file containing HR data.")
    private File hrFitFile = null;

    private static final Logger LOGGER = LogManager.getLogger(SwimEditor.class);

    private Terminal terminal;
    private final InputStream input;
    private final OutputStream output;
    private final String[] cliArgs;
    private GarminSwimActivity garminSwimActivity;
    private GarminGenericActivity hrActivity;
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
        Utils.setLogLevel(reusableOptionsMixin);

        try {
            float poolLength = 0f;
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
            }

            terminal.writer().append(garminSwimActivity.getActivitySummaryHeader());
            terminal.writer().append(garminSwimActivity.getActivitySummary());

            // Add the HR data to the swimming activity
            if (hrFitFile != null) {
                hrActivity = new GarminGenericActivity();
                GarminActivityLoader hrLoader = new GarminActivityLoader(hrFitFile, hrActivity);
                addHrDataToSwimActivity();
            }

            // Generate the newly updated FIT file
            if (editMode || hrFitFile != null) {
                FitWriter fr = new FitWriter(garminSwimActivity, swimmingFitFile.getName());
                String updatedFitFileName = fr.writeFitFile();

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

            if (reusableOptionsMixin.getVerbosity().length > 0) {
                ex.printStackTrace();
            }
            return 1;
        }

        return 0;
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

        // Determine the displayed length "summary" stroke (MIXED, etc)
        SwimStroke lapSummaryStroke = null;
        for (int i = 0; i < lengths.size(); i++) {
            LengthMesg length = lengths.get(i);

            // Initial case
            if (lapSummaryStroke == null) {
                lapSummaryStroke = length.getSwimStroke();
                continue;
            }

            // If the current stroke is something difference, set the summary to "mixed"
            if (lapSummaryStroke != length.getSwimStroke()) {
                lapSummaryStroke = SwimStroke.MIXED;
                break;
            }
        }
        lap.getLapMessage().setSwimStroke(lapSummaryStroke);

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

    private void addHrDataToSwimActivity() {
        if (garminSwimActivity.getHrMessages().size() > 0) {
            throw new RuntimeException("Swimming fit file already contains HR data.");
        }

        LOGGER.log(Level.DEBUG, "Clearing out all RecordMesg entries in the swim activity");
        List<RecordMesg> clonedSwimRecordMsgs = garminSwimActivity.deleteRecordMessages();

        // Obtain the HR and "filled in" records, in order to account for some missing stretches
        List<RecordMesg> hrRecords = Utils.getFilledInRecordMsgs(hrActivity.getRecordMessages());
        DateTime activityEnd = garminSwimActivity.getSessionMesg().getTimestamp();
        for (RecordMesg mesg : hrRecords) {
            // Discard any HR messages after the activity has finished
            if (mesg.getTimestamp().compareTo(activityEnd) > 0) {
                continue;
            }
            garminSwimActivity.addRecordMessage(mesg);
        }
    }
}
