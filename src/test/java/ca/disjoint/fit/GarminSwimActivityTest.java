import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.After;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ca.disjoint.fit.GarminSwimActivity;
import ca.disjoint.fit.GarminActivityLoader;

import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

import org.jline.utils.AttributedString;

@SuppressWarnings("checkstyle:MagicNumber")
public class GarminSwimActivityTest {
    private GarminSwimActivity activity;

    private void setUpSwimData(final String filename) {
        try {
            URL url = this.getClass().getResource(filename);
            File swimFitFile = new File(url.getFile());

            activity = new GarminSwimActivity();
            GarminActivityLoader gal = new GarminActivityLoader(swimFitFile, activity);
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
    public void shouldParseBasicSwimActivity() {
        setUpSwimData("/basic-swim.fit");
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(22.86f, activity.getPoolLength(), 0.000);
        assertEquals(6, activity.getNumActivePoolLengths());
        assertEquals(2, activity.getNumTotalPoolLaps());
        assertEquals(137.17f, activity.getTotalDistance(), 0.01);
        assertEquals(294.146f, activity.getTotalElapsedTime(), 0.00);
        assertEquals(289.483f, activity.getTotalTimerTime(), 0.00);
        assertEquals(280.856f, activity.getMovingTime(), 0.001);
        assertEquals("[Lap 1]  6 lengths  (BREASTSTROKE) 04:40 (avg 03:24/100m, best 02:49/100m)"
                + System.lineSeparator() + "         Strokes: BR,BR,BR,BR,BR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(0)));
        assertEquals(0.488f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.589f, activity.getMaxSpeed(), 0.001);
        assertThat(activity.getDeviceManufacturer(), equalTo("GARMIN"));
        assertThat(activity.getDeviceName(), equalTo("VIVOACTIVE3"));
    }

    @Test
    public void shouldParseMultipleLapSwimActivity() {
        setUpSwimData("/multiple-lap-swim.fit");
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals(2451.091f, activity.getMovingTime(), 0.001);
        assertEquals(
                "[Lap 1]  10 lengths (BREASTSTROKE) 05:51 (avg 03:27/100m, best 03:05/100m)"
                        + AttributedString.stripAnsi(System.lineSeparator())
                        + "         Strokes: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(0)));
        assertEquals(
                "[Lap 7]  8 lengths  (MIXED)        04:16 (avg 03:09/100m, best 02:11/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,FR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(6)));
        assertEquals("", activity.getLapSummary(7));
        assertEquals(0.513f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.760f, activity.getMaxSpeed(), 0.001);
        assertThat(activity.getDeviceManufacturer(), equalTo("GARMIN"));
        assertThat(activity.getDeviceName(), equalTo("VIVOACTIVE3"));
    }

    @Test
    public void shouldRandomizeCreationTime() {
        setUpSwimData("/basic-swim.fit");
        long oldTimestamp = activity.getCreationTime().getTimestamp();
        activity.randomizeCreationTime();
        long newTimestamp = activity.getCreationTime().getTimestamp();
        String testStr = String.format("newTimestamp %d is out of range of oldTimestamp %d", newTimestamp,
                oldTimestamp);
        assertTrue(testStr, newTimestamp < oldTimestamp && newTimestamp >= (oldTimestamp - 100));
    }

    @Test
    public void shouldUpdatePoolLength() {
        setUpSwimData("/multiple-lap-swim.fit");
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals(2451.091f, activity.getMovingTime(), 0.001);
        assertEquals(
                "[Lap 1]  10 lengths (BREASTSTROKE) 05:51 (avg 03:27/100m, best 03:05/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(0)));
        assertEquals(
                "[Lap 7]  8 lengths  (MIXED)        04:16 (avg 03:09/100m, best 02:11/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,FR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(6)));
        assertEquals("", activity.getLapSummary(7));
        assertEquals(0.513f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.760f, activity.getMaxSpeed(), 0.001);

        // Update the pool length to 10m
        activity.updateSwimmingPoolLength(10f);
        assertEquals(10.0f, activity.getPoolLength(), 0.000);
        assertEquals(740.0f, activity.getTotalDistance(), 0.00);
        assertEquals(0.302f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.447f, activity.getMaxSpeed(), 0.001);
    }

    @Test
    public void shouldHaveIdenticalSummaryWhenPoolLengthNotUpdated() {
        setUpSwimData("/multiple-lap-swim.fit");
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals(2451.091f, activity.getMovingTime(), 0.001);
        assertEquals(
                "[Lap 1]  10 lengths (BREASTSTROKE) 05:51 (avg 03:27/100m, best 03:05/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(0)));
        assertEquals(
                "[Lap 7]  8 lengths  (MIXED)        04:16 (avg 03:09/100m, best 02:11/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,FR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(6)));
        assertEquals("", activity.getLapSummary(7));
        assertEquals(0.513f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.760f, activity.getMaxSpeed(), 0.001);

        // "update" the pool length to be the same as before
        activity.updateSwimmingPoolLength(17.0f);
        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());
        assertEquals(17.0f, activity.getPoolLength(), 0.000);
        assertEquals(74, activity.getNumActivePoolLengths());
        assertEquals(14, activity.getNumTotalPoolLaps());
        assertEquals(1258.0f, activity.getTotalDistance(), 0.00);
        assertEquals(2773.603f, activity.getTotalElapsedTime(), 0.001);
        assertEquals(2769.107f, activity.getTotalTimerTime(), 0.001);
        assertEquals(2451.091f, activity.getMovingTime(), 0.001);
        assertEquals(
                "[Lap 1]  10 lengths (BREASTSTROKE) 05:51 (avg 03:27/100m, best 03:05/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,BR,BR,BR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(0)));
        assertEquals(
                "[Lap 7]  8 lengths  (MIXED)        04:16 (avg 03:09/100m, best 02:11/100m)" + System.lineSeparator()
                        + "         Strokes: BR,BR,BR,BR,BR,BR,FR,BR",
                AttributedString.stripAnsi(activity.getLapSummary(6)));
        assertEquals("", activity.getLapSummary(7));
        assertEquals(0.513f, activity.getAvgSpeed(), 0.001);
        assertEquals(0.760f, activity.getMaxSpeed(), 0.001);
    }

    @Test
    public void shouldDisplayNonActiveSwimLengths() {
        setUpSwimData("/1_2700_3932192586.fit");

        assertEquals(Sport.SWIMMING, activity.getSport());
        assertEquals(SubSport.LAP_SWIMMING, activity.getSubSport());

        String lap1 = "[Lap 1]  15 lengths (BREASTSTROKE) 08:28 (avg 03:52/100m, best 02:41/100m)"
                + AttributedString.stripAnsi(System.lineSeparator())
                + "         Strokes: INV,BR,BR,BR,BR,BR,BR,BR,BR,BR,BR,BR,BR,BR,BR";
        assertThat(AttributedString.stripAnsi(activity.getLapSummary(0)), equalTo(lap1));

        String lap3 = "[Lap 3]  3 lengths  (MIXED)        02:04 (avg 03:30/100m, best 03:13/100m)"
                + AttributedString.stripAnsi(System.lineSeparator()) + "         Strokes: FR,BR,INV";
        assertThat(AttributedString.stripAnsi(activity.getLapSummary(2)), equalTo(lap3));

        String lap6 = "[Lap 6]  3 lengths  (MIXED)        01:59 (avg 03:34/100m, best 03:28/100m)"
                + AttributedString.stripAnsi(System.lineSeparator()) + "         Strokes: FR,BR,INV";
        assertThat(AttributedString.stripAnsi(activity.getLapSummary(5)), equalTo(lap6));
    }
}
