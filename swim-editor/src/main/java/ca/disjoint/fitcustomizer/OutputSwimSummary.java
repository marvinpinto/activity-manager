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
            throw new RuntimeException(
                    String.format("Could not open file \"%s\" - %s", fitFile.getName(), ex.getMessage()), ex);
        }
    }
}
