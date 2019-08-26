package ca.disjoint.fitcustomizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.lang.reflect.Method;

import com.garmin.fit.Decode;
import com.garmin.fit.Mesg;
import com.garmin.fit.Field;

import org.apache.commons.lang3.text.WordUtils;

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
                LOGGER.log(Level.DEBUG, String.format("    %s: %s", fn, output));
            } catch (Exception ex) {
                LOGGER.log(Level.TRACE, "Getter method name " + fn + " for field " + f.getName()
                        + "appears to be incorrect, moving on.");
            }

        }
    }
}
