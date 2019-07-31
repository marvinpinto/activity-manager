import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.net.URL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ca.disjoint.fitcustomizer.GarminActivity;
import ca.disjoint.fitcustomizer.GarminSwimActivity;
import ca.disjoint.fitcustomizer.GarminActivityLoader;

import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

public class GarminSwimActivityTest {
    private GarminSwimActivity activity;

    private void setUpBasicSwimData() {
        try {
            URL url = this.getClass().getResource("/basic-swim.fit");
            File basicSwimFitFile = new File(url.getFile());

            activity = new GarminSwimActivity();
            GarminActivityLoader gal = new GarminActivityLoader(basicSwimFitFile, activity);
        } catch (FileNotFoundException ex) {
            fail(ex.getMessage());
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    private void setUpMultipleLapSwimData() {
        try {
            URL url = this.getClass().getResource("/multiple-lap-swim.fit");
            File basicSwimFitFile = new File(url.getFile());

            activity = new GarminSwimActivity();
            GarminActivityLoader gal = new GarminActivityLoader(basicSwimFitFile, activity);
        } catch (FileNotFoundException ex) {
            fail(ex.getMessage());
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    @After
    public void tearDown() {
        activity = null;
    }

    @Test
    public void shouldParseBasicSwimActivityCorrectly() {
        setUpBasicSwimData();
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(22.86f, activity.getPoolLength(), 0.000);
        assertEquals(6, activity.getNumActivePoolLengths());
        assertEquals(2, activity.getNumTotalPoolLaps());
        assertEquals(137.17f, activity.getTotalDistance(), 0.01);
        assertEquals(294.146f, activity.getTotalElapsedTime(), 0.00);
        assertEquals(289.483f, activity.getTotalTimerTime(), 0.00);
        assertEquals("[Lap 1]   6 lengths (BREASTSTROKE) 00:04:40: BR,BR,BR,BR,BR,BR", activity.getLapSummary(0));
    }

    @Test
    public void shouldParseMultipleLapSwimActivityCorrectly() {
        setUpMultipleLapSwimData();
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals("[Lap 1]  10 lengths (BREASTSTROKE) 00:05:51: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                activity.getLapSummary(0));
        assertEquals("[Lap 7]   8 lengths (MIXED)        00:04:16: BR,BR,BR,BR,BR,BR,FR,BR", activity.getLapSummary(6));
        assertEquals("", activity.getLapSummary(7));
    }

    @Test
    public void shouldRandomizeCreationTime() {
        setUpBasicSwimData();
        long oldTimestamp = activity.getCreationTime().getTimestamp();
        activity.randomizeCreationTime();
        long newTimestamp = activity.getCreationTime().getTimestamp();
        String testStr = String.format("newTimestamp %d is out of range of oldTimestamp %d", newTimestamp,
                oldTimestamp);
        assertTrue(testStr, newTimestamp < oldTimestamp && newTimestamp >= (oldTimestamp - 100));
    }

    @Test
    public void shouldUpdatePoolLength() {
        setUpMultipleLapSwimData();
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals("[Lap 1]  10 lengths (BREASTSTROKE) 00:05:51: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                activity.getLapSummary(0));
        assertEquals("[Lap 7]   8 lengths (MIXED)        00:04:16: BR,BR,BR,BR,BR,BR,FR,BR", activity.getLapSummary(6));
        assertEquals("", activity.getLapSummary(7));

        // Update the pool length to 10m
        activity.updateSwimmingPoolLength(10f);
        assertEquals(10.0f, activity.getPoolLength(), 0.000);
        assertEquals(740.0f, activity.getTotalDistance(), 0.00);
    }
}
