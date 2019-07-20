package ca.disjoint.fitcustomizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class OutputSwimSummary {
    private FileInputStream in;
    private static final Logger LOGGER = LogManager.getLogger("SwimEditor");

    public OutputSwimSummary(File fitFile) {
        try {
            in = new FileInputStream(fitFile);
        } catch (IOException ex) {
            String msg = String.format("Could not open file \"%s\". Error: %s", fitFile.getName(), ex.getMessage());
            LOGGER.log(Level.ERROR, msg);
            LOGGER.log(Level.DEBUG, ex);
            throw new RuntimeException(msg);
        }
    }
}
