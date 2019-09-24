/*
    Activity Manager
    Copyright (C) 2019 - Marvin Pinto

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ca.disjoint.fit;

import java.util.List;
import java.util.Iterator;
import java.util.Formatter;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LengthType;
import com.garmin.fit.SwimStroke;
import com.garmin.fit.SessionMesg;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public final class GarminSwimActivity extends GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminSwimActivity.class);

    public GarminSwimActivity() {
        super();
    }

    public float getPoolLength() {
        return getSessionMesg().getPoolLength();
    }

    public int getNumActivePoolLengths() {
        return getSessionMesg().getNumActiveLengths();
    }

    public int getNumTotalPoolLaps() {
        return getSessionMesg().getNumLaps();
    }

    public float getTotalDistance() {
        return getSessionMesg().getTotalDistance();
    }

    public float getTotalElapsedTime() {
        return getSessionMesg().getTotalElapsedTime();
    }

    public float getTotalTimerTime() {
        return getSessionMesg().getTotalTimerTime();
    }

    public float getAvgSpeed() {
        return getSessionMesg().getAvgSpeed();
    }

    public float getMaxSpeed() {
        return getSessionMesg().getMaxSpeed();
    }

    public float getMovingTime() {
        float movingTime = 0f;
        List<GarminLap> garminLaps = getGarminLaps();

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

    public void recalculateActivityStats() {
        LOGGER.log(Level.DEBUG, "Recalculating Garmin activity stats");
        float sessionTotalDistance = 0f;
        float sessionTotalSpeed = 0f;
        float sessionMaxSpeed = 0f;
        float poolLength = getPoolLength();
        int activeLaps = 0;
        List<GarminLap> garminLaps = getGarminLaps();
        SessionMesg sessionMesg = getSessionMesg();

        for (GarminLap garminLap : garminLaps) {
            int lapNumActiveLengths = 0;
            float lapMaxSpeed = 0f;
            LapMesg lap = garminLap.getLapMessage();

            // Ignore any "rest" laps
            if (lap.getSwimStroke() == null) {
                continue;
            }
            activeLaps++;

            for (LengthMesg length : garminLap.getLengthMessages()) {
                // Ignore any non-active lengths
                if (length.getLengthType() != LengthType.ACTIVE) {
                    continue;
                }

                // Update the number of "active lengths" in this lap
                lapNumActiveLengths++;

                // Update the avg speed to reflect the pool length
                float avgSpeed = poolLength / length.getTotalTimerTime();
                length.setAvgSpeed(avgSpeed);

                // Update the lap max speed, if applicable
                if (avgSpeed > lapMaxSpeed) {
                    lapMaxSpeed = avgSpeed;
                }

                // Update the session max speed, if applicable
                if (avgSpeed > sessionMaxSpeed) {
                    sessionMaxSpeed = avgSpeed;
                }
            }

            // Set the number of active lengths for this lap
            lap.setNumActiveLengths(lapNumActiveLengths);

            // Set the total lap distance
            float lapTotalDistance = lap.getNumActiveLengths() * poolLength;
            lap.setTotalDistance(lapTotalDistance);

            // Calculate the avg/max speed metrics for this lap
            float lapAvgSpeed = lapTotalDistance / lap.getTotalElapsedTime();
            lap.setAvgSpeed(lapAvgSpeed);
            lap.setMaxSpeed(lapMaxSpeed);
            lap.setEnhancedAvgSpeed(lapAvgSpeed);
            lap.setEnhancedMaxSpeed(lapMaxSpeed);

            // Increment the session distance
            sessionTotalDistance += lapTotalDistance;

            // Increment the session speed
            LOGGER.log(Level.DEBUG, "Lap avg speed: " + lapAvgSpeed);
            sessionTotalSpeed += lapAvgSpeed;

            // Set the lap avg stroke distance
            float lapAvgStrokeDistance = lap.getTotalDistance() / lap.getTotalCycles();
            lap.setAvgStrokeDistance(lapAvgStrokeDistance);
        }

        // Update the session metrics to account for the pool length
        sessionMesg.setTotalDistance(sessionTotalDistance);
        LOGGER.log(Level.DEBUG, "Session total speed: " + sessionTotalSpeed + ", laps: " + activeLaps);
        float sessionAvgSpeed = sessionTotalSpeed / activeLaps;
        LOGGER.log(Level.DEBUG, "Session avg speed: " + sessionAvgSpeed);
        sessionMesg.setAvgSpeed(sessionAvgSpeed);
        sessionMesg.setMaxSpeed(sessionMaxSpeed);
        sessionMesg.setEnhancedAvgSpeed(sessionAvgSpeed);
        sessionMesg.setEnhancedMaxSpeed(sessionMaxSpeed);
        sessionMesg.setAvgStrokeDistance(sessionMesg.getTotalDistance() / sessionMesg.getTotalCycles());

        // Update the session message obj
        setSessionMesg(sessionMesg);
    }

    public void updateSwimmingPoolLength(final float newPoolLength) {
        SessionMesg sessionMesg = getSessionMesg();
        sessionMesg.setPoolLength(newPoolLength);
        setSessionMesg(sessionMesg);

        recalculateActivityStats();
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

        // e.g. Device: Garmin Vivoactive3
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Device:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s %s", Utils.titleCaseString(getDeviceManufacturer()),
                Utils.titleCaseString(getDeviceName())));
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
        asb.append(String.format(" %.0fm", getSessionMesg().getTotalDistance()));
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
        asb.append(String.format(" %s/100m",
                Utils.convertFloatToStringDate(Utils.PACE_PER_HUNDRED_METERS / getAvgSpeed())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // e.g. Best pace: 04:41/100m
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append(String.format("%-15s", "Best pace:"));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format(" %s/100m",
                Utils.convertFloatToStringDate(Utils.PACE_PER_HUNDRED_METERS / getMaxSpeed())));
        asb.append(System.lineSeparator());
        sb.append(asb.toAnsi());

        // Append the Lap Summary data
        sb.append(System.lineSeparator());
        sb.append(getLapSummary());
        return sb.toString();
    }

    public List<Integer> getActiveSwimLaps() {
        List<Integer> activeSwimLaps = new ArrayList<Integer>();
        List<GarminLap> garminLaps = getGarminLaps();

        for (int i = 0; i < garminLaps.size(); i++) {
            GarminLap garminLap = garminLaps.get(i);
            LapMesg lap = garminLap.getLapMessage();

            // Ignore any "rest" laps
            if (lap.getSwimStroke() == null) {
                continue;
            }

            activeSwimLaps.add(i);
        }

        return activeSwimLaps;
    }

    public String getLapSummary(final int lapIndex) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        List<GarminLap> garminLaps = getGarminLaps();

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
        asb.append(String.format("%-10s ", lap.getNumLengths() + " lengths"));
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append(String.format("%-14s ", "(" + lap.getSwimStroke() + ")"));
        asb.append(String.format("%s ", Utils.convertFloatToStringDate(lap.getTotalTimerTime())));
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        asb.append(String.format("(avg %s/100m, best %s/100m)%n",
                Utils.convertFloatToStringDate(Utils.PACE_PER_HUNDRED_METERS / lap.getAvgSpeed()),
                Utils.convertFloatToStringDate(Utils.PACE_PER_HUNDRED_METERS / lap.getMaxSpeed())));

        // Stroke list
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        asb.append(String.format("%-8s Strokes: ", ""));
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        for (Iterator i = lengths.iterator(); i.hasNext();) {
            LengthMesg len = (LengthMesg) i.next();
            if (len.getLengthType() != LengthType.ACTIVE) {
                asb.append(GarminSwimStroke.getByValue(SwimStroke.INVALID.getValue()).toString());
            } else {
                asb.append(GarminSwimStroke.getByValue(len.getSwimStroke().getValue()).toString());
            }
            if (i.hasNext()) {
                asb.append(",");
            }
        }

        return asb.toAnsi();
    }

    public String getLapSummary() {
        StringBuilder sb = new StringBuilder();
        List<GarminLap> garminLaps = getGarminLaps();

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
