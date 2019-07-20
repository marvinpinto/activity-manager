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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import ca.disjoint.fitcustomizer.OutputSwimSummary;

@Command(name = "SwimEditor.jar", mixinStandardHelpOptions = true, versionProvider = SwimEditor.PropertiesVersionProvider.class, description = "Edit Garmin swim .fit files to add heartrate data, correct strokes, and more.")
public class SwimEditor implements Runnable {
    @Option(names = { "-v", "--verbose" }, description = "Verbose mode. Helpful for troubleshooting. ")
    private boolean verbose = false;

    @Parameters(arity = "1", paramLabel = "FILE", description = "Swimming FIT file to process.")
    private File swimmingFitFile;

    private static final Logger LOGGER = LogManager.getLogger("SwimEditor");

    public void run() {
        if (verbose) {
            Configurator.setLevel("SwimEditor", Level.DEBUG);
            LOGGER.log(Level.INFO, "Verbose logging enabled");
        }

        OutputSwimSummary summary = new OutputSwimSummary(swimmingFitFile);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SwimEditor()).execute(args);
        System.exit(exitCode);
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
