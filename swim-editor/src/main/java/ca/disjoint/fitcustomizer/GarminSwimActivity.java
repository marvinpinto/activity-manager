package ca.disjoint.fitcustomizer;

import java.util.List;
import java.util.Iterator;
import java.util.Formatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.LengthType;

import ca.disjoint.fitcustomizer.GarminActivity;
import ca.disjoint.fitcustomizer.GarminSwimStroke;
import ca.disjoint.fitcustomizer.GarminLap;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class GarminSwimActivity extends GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminSwimActivity.class);

    public GarminSwimActivity() {
    }

    public float getPoolLength() {
        return sessionMesg.getPoolLength();
    }

    public int getNumActivePoolLengths() {
        return sessionMesg.getNumActiveLengths();
    }

    public int getNumTotalPoolLaps() {
        return sessionMesg.getNumLaps();
    }

    public float getTotalDistance() {
        return sessionMesg.getTotalDistance();
    }

    public float getTotalElapsedTime() {
        return sessionMesg.getTotalElapsedTime();
    }

    public float getTotalTimerTime() {
        return sessionMesg.getTotalTimerTime();
    }

    public float getAvgSpeed() {
        return sessionMesg.getAvgSpeed();
    }

    public float getMaxSpeed() {
        return sessionMesg.getMaxSpeed();
    }

    public float getMovingTime() {
        float movingTime = 0f;

        for (GarminLap garminLap : garminLaps) {
            // Ignore any "rest" laps
            LapMesg lap = garminLap.getLapMessage();
            if (lap.getSwimStroke() == null) {
                continue;
            }

            for (LengthMesg length : garminLap.getLengthMessages()) {
                // Ignore any non-active lengths
                if (length.getLengthType() != LengthType.ACTIVE) {
                    continue;
                }

                // Increment the moving time to account for this swim length
                movingTime += length.getTotalTimerTime();
            }
        }

        return movingTime;
    }

    public void updateSwimmingPoolLength(float newPoolLength) {
        LOGGER.log(Level.DEBUG, "Updating pool length to: " + newPoolLength);
        float sessionTotalDistance = 0f;
        float sessionTotalSpeed = 0f;
        float sessionMaxSpeed = 0f;

        for (GarminLap garminLap : garminLaps) {
            float lapMaxSpeed = 0f;
            float lapTotalSpeed = 0f;
            LapMesg lap = garminLap.getLapMessage();

            // Ignore any "rest" laps
            if (lap.getSwimStroke() == null) {
                continue;
            }

            for (LengthMesg length : garminLap.getLengthMessages()) {
                // Ignore any non-active lengths
                if (length.getLengthType() != LengthType.ACTIVE) {
                    continue;
                }

                // Update the avg speed to reflect the new pool length
                float avgSpeed = newPoolLength / length.getTotalTimerTime();
                length.setAvgSpeed(avgSpeed);

                // Update the lap max speed, if applicable
                if (avgSpeed > lapMaxSpeed) {
                    lapMaxSpeed = avgSpeed;
                }

                // Update the session max speed, if applicable
                if (avgSpeed > sessionMaxSpeed) {
                    sessionMaxSpeed = avgSpeed;
                }

                // Increment the lap total speed
                lapTotalSpeed += avgSpeed;
            }

            // Calculate the avg/max speed metrics for this lap
            float lapAvgSpeed = lapTotalSpeed / lap.getNumActiveLengths();
            lap.setAvgSpeed(lapAvgSpeed);
            lap.setMaxSpeed(lapMaxSpeed);
            lap.setEnhancedAvgSpeed(lapAvgSpeed);
            lap.setEnhancedMaxSpeed(lapMaxSpeed);

            // Set the total lap distance
            float lapTotalDistance = lap.getNumActiveLengths() * newPoolLength;
            lap.setTotalDistance(lapTotalDistance);

            // Increment the session distance
            sessionTotalDistance += lapTotalDistance;

            // Increment the session speed
            LOGGER.log(Level.DEBUG, "Lap avg speed: " + lapAvgSpeed);
            sessionTotalSpeed += lapAvgSpeed;

            // Set the lap avg stroke distance
            float lapAvgStrokeDistance = lap.getTotalDistance() / lap.getTotalCycles();
            lap.setAvgStrokeDistance(lapAvgStrokeDistance);
        }

        // Update the session metrics to account for the new pool length
        sessionMesg.setTotalDistance(sessionTotalDistance);
        LOGGER.log(Level.DEBUG, "Session total speed: " + sessionTotalSpeed + ", laps: " + sessionMesg.getNumLaps());
        float sessionAvgSpeed = sessionTotalSpeed / sessionMesg.getNumLaps();
        LOGGER.log(Level.DEBUG, "Session avg speed: " + sessionAvgSpeed);
        sessionMesg.setAvgSpeed(sessionAvgSpeed);
        sessionMesg.setMaxSpeed(sessionMaxSpeed);
        sessionMesg.setEnhancedAvgSpeed(sessionAvgSpeed);
        sessionMesg.setEnhancedMaxSpeed(sessionMaxSpeed);
        sessionMesg.setAvgStrokeDistance(sessionMesg.getTotalDistance() / sessionMesg.getTotalCycles());
        sessionMesg.setPoolLength(newPoolLength);
    }

    @Override
    public String getActivitySummary() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        AttributedStringBuilder asb;

        // e.g. Sport: Swimming (LapSwimming)
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Sport:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s (%s)", Utils.titleCaseString(getSport().toString()),
                Utils.titleCaseString(getSubSport().toString())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Date: Fri Jun 21 07:01:37 EDT 2019
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Date:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s", getCreationTime()));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Pool length: 15.24m
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Pool length:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s", getPoolLength() + "m"));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Total lengths: 68 (10 laps)
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Total lengths:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %d (%d laps)", getNumActivePoolLengths(), getNumTotalPoolLaps()));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Distance: 1042m
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Distance:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %.0fm", sessionMesg.getTotalDistance()));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Timer time: 00:10:11
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Timer time:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s", Utils.convertFloatToStringDate(getTotalTimerTime())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Elapsed time: 00:12:11
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Elapsed time:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s", Utils.convertFloatToStringDate(getTotalElapsedTime())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Moving time: 00:10:11
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Moving time:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s", Utils.convertFloatToStringDate(getMovingTime())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Avg pace: 04:41/100m
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Avg pace:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s/100m", Utils.convertFloatToStringDate(100 / getAvgSpeed())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Best pace: 04:41/100m
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Best pace:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s/100m", Utils.convertFloatToStringDate(100 / getMaxSpeed())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // Append the Lap Summary data
        sb.append(System.lineSeparator());
        sb.append(getLapSummary());
        return sb.toString();
    }

    public String getLapSummary(int lapIndex) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        GarminLap garminLap = garminLaps.get(lapIndex);
        LapMesg lap = garminLap.getLapMessage();
        List<LengthMesg> lengths = garminLap.getLengthMessages();

        // Ignore any "rest" laps
        if (lap.getSwimStroke() == null) {
            return "";
        }

        // Summary portion
        int internalLapIndex = lap.getMessageIndex() + 1;
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append(String.format("%-8s ", "[Lap " + internalLapIndex + "]"));
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-10s ", lap.getNumActiveLengths() + " lengths"));
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append(String.format("%-14s ", "(" + lap.getSwimStroke() + ")"));
        asb.append(String.format("%s ", Utils.convertFloatToStringDate(lap.getTotalTimerTime())));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(
                String.format("(avg %s/100m, best %s/100m)%n", Utils.convertFloatToStringDate(100 / lap.getAvgSpeed()),
                        Utils.convertFloatToStringDate(100 / lap.getMaxSpeed())));

        // Stroke list
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append(String.format("%-8s Strokes: ", ""));
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        for (Iterator i = lengths.iterator(); i.hasNext();) {
            LengthMesg len = (LengthMesg) i.next();
            asb.append(GarminSwimStroke.getByValue(len.getSwimStroke().getValue()).toString());
            if (i.hasNext()) {
                asb.append(",");
            }
        }

        return asb.toAnsi();
    }

    public String getLapSummary() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < garminLaps.size(); i++) {
            String summary = getLapSummary(i);
            sb.append(summary);
            if (!summary.isEmpty()) {
                sb.append(System.lineSeparator());
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
}
