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

import ca.disjoint.fitcustomizer.OutputSwimSummary;

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

    public Integer call() {
        if (verbose) {
            Configurator.setRootLevel(Level.DEBUG);
            LOGGER.log(Level.INFO, "Verbose logging enabled");
        }

        try {
            OutputSwimSummary summary = new OutputSwimSummary(swimmingFitFile, editMode, randomizeCreationTime);
            System.out.println();
            System.out.println("============");
            System.out.println("Summary Data");
            System.out.println("============");
            System.out.println(summary.getSummaryData());
            LOGGER.log(Level.DEBUG, "Swim summary output complete");
        } catch (Exception ex) {
            String msg = String.format("Error: %s", ex.getMessage());
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
