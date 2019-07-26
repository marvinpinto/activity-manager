import static org.junit.Assert.assertEquals;
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

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        URL url = this.getClass().getResource("/basic-swim.fit");
        File basicSwimFitFile = new File(url.getFile());

        activity = new GarminSwimActivity();
        GarminActivityLoader gal = new GarminActivityLoader(basicSwimFitFile, activity);
    }

    @After
    public void tearDown() {
        activity = null;
    }

    @Test
    public void shouldParseSwimActivityCorrectly() {
        System.out.println(activity.getActivitySummary());
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
}
