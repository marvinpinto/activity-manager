package ca.disjoint.fitcustomizer;

import java.io.File;
import java.time.Instant;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.BufferEncoder;

import ca.disjoint.fitcustomizer.Utils;

public class FitWriter {
    private static final Logger LOGGER = LogManager.getLogger(FitWriter.class);
    private BufferEncoder inputFitFile;
    private String originalFileName;

    public FitWriter(BufferEncoder fitFile, String fileName) {
        inputFitFile = fitFile;
        originalFileName = fileName;
    }

    public String writeFitFile() throws FileNotFoundException, IOException {
        File newFitFile = getNewFileHandle(originalFileName);
        byte[] rawBytes = inputFitFile.close();
        OutputStream os = new FileOutputStream(newFitFile);

        LOGGER.log(Level.DEBUG, "Writing updated FIT contents out to file " + newFitFile.getName());
        os.write(rawBytes);
        os.close();

        boolean fitIntegrityStatus = Utils.checkFitFileIntegrity(newFitFile);
        if (!fitIntegrityStatus) {
            throw new RuntimeException("FIT file integrity check failed");
        }

        return newFitFile.getName();
    }

    private File getNewFileHandle(String originalFileName) {
        LOGGER.log(Level.DEBUG, "Determining file name for update FIT file (original: " + originalFileName + ")");

        String[] tokens = originalFileName.split("\\.(?=[^\\.]+$)");
        String base = tokens[0];
        String ext = tokens[1];
        Instant now = Instant.now();
        String newFileName = base + "-" + now.getEpochSecond() + "." + ext;
        LOGGER.log(Level.DEBUG, "Checking to see if filename \"" + newFileName + "\" is useable");

        File newFile = new File(newFileName);
        if (newFile.exists()) {
            throw new RuntimeException("File \"" + newFileName + "\" already exists and will not be overwritten");
        }

        LOGGER.log(Level.DEBUG, "Filename \"" + newFileName + "\" appears to be useable");
        return newFile;
    }

}
