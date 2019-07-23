package ca.disjoint.fitcustomizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.garmin.fit.Decode;

public final class Utils {
    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    public static boolean checkFitFileIntegrity(File file) throws FileNotFoundException, IOException {
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
}
