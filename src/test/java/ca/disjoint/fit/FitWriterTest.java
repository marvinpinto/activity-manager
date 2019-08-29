import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.After;

import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;
import java.io.FileNotFoundException;

import java.nio.file.Paths;

import ca.disjoint.fit.FitWriter;
import ca.disjoint.fit.TestUtils;
import ca.disjoint.fit.GarminSwimActivity;
import ca.disjoint.fit.GarminActivityLoader;

import com.garmin.fit.HrMesg;

@SuppressWarnings("checkstyle:MagicNumber")
public class FitWriterTest {
    private FitWriter inst;

    private void validateHrMesgRecords(final String testFitFileName, final boolean shouldContainHrMesg)
            throws URISyntaxException, FileNotFoundException, IOException {
        URL url = this.getClass().getResource(testFitFileName);
        File inputFitFile = Paths.get(url.toURI()).toFile();
        GarminSwimActivity garminSwimActivity = new GarminSwimActivity();
        GarminActivityLoader gal = new GarminActivityLoader(inputFitFile, garminSwimActivity);

        if (shouldContainHrMesg) {
            assertTrue("Should contain one or more HrMesg entries", garminSwimActivity.getHrMessages().size() > 0);
        }
        for (HrMesg mesg : garminSwimActivity.getHrMessages()) {
            assertTrue("Number of event_timestamp entries should be <=8, got: " + mesg.getNumEventTimestamp(),
                    mesg.getNumEventTimestamp() <= 8);
            assertTrue("Number of filtered_bpm entries should be <=8, got: " + mesg.getNumFilteredBpm(),
                    mesg.getNumFilteredBpm() <= 8);
            assertTrue("Number of event_timestamp_12 entries should be <=12, got: " + mesg.getNumEventTimestamp12(),
                    mesg.getNumEventTimestamp12() <= 12);
            assertTrue(
                    "Should be equal number of timestamp & filtered_bpm entries. Got " + mesg.getNumEventTimestamp()
                            + " timestamp entries & " + mesg.getNumFilteredBpm() + " bpm entries.",
                    mesg.getNumEventTimestamp() == mesg.getNumFilteredBpm());
        }

        // Write out the activity to a new fit file
        FitWriter fr = new FitWriter(garminSwimActivity, inputFitFile.getName());
        String outputFitFileName = fr.writeFitFile();
        File outputFitFile = new File(outputFitFileName);
        assertTrue("Updated fit file " + outputFitFileName + " did not get created", outputFitFile.exists());

        // Re-read the generated FIT file and assert its contents
        garminSwimActivity = new GarminSwimActivity();
        gal = new GarminActivityLoader(outputFitFile, garminSwimActivity);

        if (shouldContainHrMesg) {
            assertTrue("Should contain one or more HrMesg entries", garminSwimActivity.getHrMessages().size() > 0);
        }
        for (HrMesg mesg : garminSwimActivity.getHrMessages()) {
            assertTrue("Number of event_timestamp entries should be <=8, got: " + mesg.getNumEventTimestamp(),
                    mesg.getNumEventTimestamp() <= 8);
            assertTrue("Number of filtered_bpm entries should be <=8, got: " + mesg.getNumFilteredBpm(),
                    mesg.getNumFilteredBpm() <= 8);
            assertTrue("Number of event_timestamp_12 entries should be <=12, got: " + mesg.getNumEventTimestamp12(),
                    mesg.getNumEventTimestamp12() <= 12);
            assertTrue(
                    "Should be equal number of timestamp & filtered_bpm entries. Got " + mesg.getNumEventTimestamp()
                            + " timestamp entries & " + mesg.getNumFilteredBpm() + " bpm entries.",
                    mesg.getNumEventTimestamp() == mesg.getNumFilteredBpm());
        }
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.deleteAllTestGeneratedFitFiles();
    }

    @Test
    public void shouldWriteCorrectNumberOfHrMesgRecords()
            throws URISyntaxException, FileNotFoundException, IOException {
        validateHrMesgRecords("/1_2700_sample-run.fit", false);
        validateHrMesgRecords("/1_2700_20190621-swim.fit", false);
        validateHrMesgRecords("/1_1765-20151021-sample-swim-with-hr.fit", true);
        validateHrMesgRecords("/1_1765-20151123-sample-swim-with-hr.fit", true);
    }
}
