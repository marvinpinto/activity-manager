package ca.disjoint.fitcustomizer;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Formatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LapMesg;

import ca.disjoint.fitcustomizer.GarminActivity;
import ca.disjoint.fitcustomizer.GarminSwimStroke;

public class GarminSwimActivity extends GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminSwimActivity.class);
    private float swimmingPoolLength = 0f;

    public GarminSwimActivity() {
    }

    public void updateSwimmingPoolLength(float length) {
        swimmingPoolLength = length;
    }

    public String getActivitySummary() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<LapMesg, List<LengthMesg>> laps : garminLaps.entrySet()) {
            LapMesg lap = laps.getKey();
            List<LengthMesg> lengths = laps.getValue();

            // Filter out any "rest" laps from the activity summary
            if (lap.getSwimStroke() == null) {
                continue;
            }

            // Summary portion
            int lapIndex = lap.getMessageIndex() + 1;
            Formatter fmt = new Formatter(sb);
            fmt.format("%-8s ", "[Lap " + lapIndex + "]");
            fmt.format("%10s ", lap.getNumActiveLengths() + " lengths");
            fmt.format("%-14s ", "(" + lap.getSwimStroke() + ")");
            fmt.format("%8s: ", Utils.convertFloatToStringDate(lap.getTotalTimerTime()));

            // Stroke list
            for (Iterator i = lengths.iterator(); i.hasNext();) {
                LengthMesg len = (LengthMesg) i.next();
                sb.append(GarminSwimStroke.getByValue(len.getSwimStroke().getValue()));
                if (i.hasNext()) {
                    sb.append(",");
                }
            }

            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

}
