package ca.disjoint.fit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.ArrayList;

import com.garmin.fit.Decode;
import com.garmin.fit.Mesg;
import com.garmin.fit.Field;
import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;

import org.apache.commons.text.WordUtils;

public final class Utils {
    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    public static final int PACE_PER_HUNDRED_METERS = 100;

    protected Utils() {
        throw new UnsupportedOperationException();
    }

    public static boolean checkFitFileIntegrity(final String file) throws FileNotFoundException, IOException {
        File f = new File(file);
        return checkFitFileIntegrity(f);
    }

    public static boolean checkFitFileIntegrity(final File file) throws FileNotFoundException, IOException {
        Decode decode = new Decode();

        LOGGER.log(Level.DEBUG, "Opening input file " + file.getName() + " in order to verify FIT file integrity");
        FileInputStream in = new FileInputStream(file);

        LOGGER.log(Level.DEBUG, "Checking FIT file integrity");
        boolean fitIntegrityStatus = decode.checkFileIntegrity((InputStream) in);
        if (!fitIntegrityStatus) {
            LOGGER.log(Level.ERROR, "FIT file integrity check failed");
        }
        in.close();

        LOGGER.log(Level.DEBUG, "FIT file integrity check successful");
        return fitIntegrityStatus;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public static String convertFloatToStringDate(final float value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));
        Instant instant = Instant.ofEpochMilli((long) (value * 1000));
        String retval = formatter.format(instant);

        // Strip any (leading) empty hour values
        if (retval.startsWith("00:")) {
            retval = retval.substring(3);
        }
        return retval;
    }

    public static String titleCaseString(final String input) {
        String op = input;
        op = op.replaceAll("_", " ");
        op = WordUtils.capitalizeFully(op);
        op = op.replaceAll(" ", "");
        LOGGER.log(Level.TRACE, "Title-cased raw string " + input + " to " + op);
        return op;
    }

    public static void logFitMessage(final Mesg mesg) {
        String msgName = mesg.getName();
        LOGGER.log(Level.TRACE, "Raw message name:" + msgName);
        Class<?> classType = null;

        try {
            // Convert it into something we can use to determine the subclass
            msgName = titleCaseString(msgName) + "Mesg";
            LOGGER.log(Level.TRACE, "Converted string into Garmin class name: " + msgName);
            classType = Class.forName("com.garmin.fit." + msgName);

            LOGGER.log(Level.DEBUG, "Message: " + msgName);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.TRACE, "Could not determine class type for message: " + msgName);
        }

        for (Field f : mesg.getFields()) {
            // Attempt to determine the getter using the raw field name
            String fn = "get" + titleCaseString(f.getName());

            try {
                // Attempt to invoke the getter on the casted object
                Method getter = classType.getDeclaredMethod(fn);
                Object output = getter.invoke(mesg);

                // Convert the array output into something printable
                if (output.getClass().isArray()) {
                    Object[] arrOutput = (Object[]) output;
                    output = Arrays.toString(arrOutput);
                }

                LOGGER.log(Level.DEBUG, String.format("    %s: %s", fn, output));

                // Special case for getters that return a Garmin DateTime object
                if (output instanceof DateTime) {
                    LOGGER.log(Level.TRACE,
                            String.format("Output \"%s\" appears to be a Garmin DateTime object", output));
                    DateTime d = (DateTime) output;
                    LOGGER.log(Level.DEBUG, String.format("       - raw timestamp: %s", d.getTimestamp()));
                }
            } catch (Exception ex) {
                LOGGER.log(Level.TRACE, "Error: " + ex.getMessage());
                LOGGER.log(Level.TRACE, "Getter method name " + fn + " for field " + f.getName()
                        + " appears to be incorrect, moving on.");
            }

        }
    }

    public static void setLogLevel(final ReusableOptions mixin) {
        if (mixin.getVerbosity().length == 1) {
            Configurator.setRootLevel(Level.DEBUG);
            LOGGER.log(Level.INFO, "DEBUG logging enabled");
        } else if (mixin.getVerbosity().length >= 2) {
            Configurator.setRootLevel(Level.TRACE);
            LOGGER.log(Level.INFO, "TRACE logging enabled");
        } else {
            Configurator.setRootLevel(Level.WARN);
            LOGGER.log(Level.INFO, "WARN logging enabled");
        }
    }

    public static List<RecordMesg> getFilledInRecordMsgs(final List<RecordMesg> hrRecords) {
        List<RecordMesg> fillerRecords = new ArrayList<RecordMesg>();

        // Clone & sort the incoming records, by timestamp
        // Note that we only really care about the timestamp & heartrate fields here
        List<RecordMesg> records = new ArrayList<RecordMesg>();
        for (RecordMesg rec : hrRecords) {
            if (rec.getHeartRate() == null) {
                // Ignore any non-HR records
                continue;
            }
            RecordMesg newRec = new RecordMesg();
            newRec.setTimestamp(rec.getTimestamp());
            newRec.setHeartRate(rec.getHeartRate());
            records.add(newRec);
        }
        records.sort(new GarminDateTimeComparator<RecordMesg>());

        for (int i = 1; i < records.size(); i++) {
            RecordMesg prevMesg = records.get(i - 1);
            RecordMesg mesg = records.get(i);

            DateTime prevTs = prevMesg.getTimestamp();
            DateTime currTs = mesg.getTimestamp();
            long tsDifference = currTs.getTimestamp() - prevTs.getTimestamp();

            if (tsDifference <= 1) {
                continue;
            }

            LOGGER.log(Level.INFO, "Generating " + tsDifference + " filler HR records to account for missing time");
            short prevHr = prevMesg.getHeartRate();
            short currHr = mesg.getHeartRate();
            LOGGER.log(Level.TRACE, "PrevHR: " + prevHr + ", currHr: " + currHr);
            for (int x = 1; x <= tsDifference; x++) {
                // Generate a random value between min(prevHr, currHr) and max(prevHr, currHr)
                short randHr = (short) ThreadLocalRandom.current().nextInt(Math.min(prevHr, currHr),
                        Math.max(prevHr, currHr) + 1);

                DateTime newTs = new DateTime(prevTs);
                newTs.add(x);
                RecordMesg fillerMesg = new RecordMesg();
                fillerMesg.setTimestamp(newTs);
                fillerMesg.setHeartRate(randHr);
                fillerRecords.add(fillerMesg);
            }
        }

        records.addAll(fillerRecords);
        records.sort(new GarminDateTimeComparator<RecordMesg>());
        return records;
    }
}
