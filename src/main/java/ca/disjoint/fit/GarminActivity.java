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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.FileIdMesg;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.EventMesg;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.HrMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.GarminProduct;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public abstract class GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminActivity.class);
    private FileIdMesg fileIdMesg;
    private ActivityMesg activityMesg;
    private SessionMesg sessionMesg;
    private List<GarminLap> garminLaps; // one lap has many lengths
    private List<RecordMesg> recordMessages;
    private List<EventMesg> eventMessages;
    private List<DeviceInfoMesg> deviceInfoMessages;
    private List<HrMesg> hrMessages;

    public GarminActivity() {
        garminLaps = new ArrayList<GarminLap>();
        recordMessages = new ArrayList<RecordMesg>();
        eventMessages = new ArrayList<EventMesg>();
        deviceInfoMessages = new ArrayList<DeviceInfoMesg>();
        hrMessages = new ArrayList<HrMesg>();
    }

    public final String getActivitySummaryHeader() {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN | AttributedStyle.BRIGHT));
        asb.append("==================");
        asb.append(System.lineSeparator());
        asb.append(" Activity Summary");
        asb.append(System.lineSeparator());
        asb.append("==================");
        asb.append(System.lineSeparator());
        return asb.toAnsi();
    }

    public abstract String getActivitySummary();

    public final void setFileIdMesg(final FileIdMesg mesg) {
        Utils.logFitMessage(mesg);
        fileIdMesg = mesg;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public final void randomizeCreationTime() {
        DateTime creationTime = fileIdMesg.getTimeCreated();

        Random r = new Random();
        long low = 1L;
        long high = 100L;
        long timestamp = (low + (long) (Math.random() * (high - low))) * -1;
        LOGGER.log(Level.DEBUG, "Generated random timestamp value " + timestamp);
        creationTime.add(timestamp);
        fileIdMesg.setTimeCreated(creationTime);
    }

    public final DateTime getCreationTime() {
        return fileIdMesg.getTimeCreated();
    }

    public final FileIdMesg getFileIdMesg() {
        return fileIdMesg;
    }

    public final void setActivityMesg(final ActivityMesg mesg) {
        Utils.logFitMessage(mesg);
        activityMesg = mesg;
    }

    public final ActivityMesg getActivityMesg() {
        return activityMesg;
    }

    public final void setSessionMesg(final SessionMesg mesg) {
        Utils.logFitMessage(mesg);
        sessionMesg = mesg;
    }

    public final SessionMesg getSessionMesg() {
        return sessionMesg;
    }

    public final void addGarminLap(final LapMesg lapMesg, final List<LengthMesg> lengthMessages) {
        Utils.logFitMessage(lapMesg);
        for (LengthMesg m : lengthMessages) {
            Utils.logFitMessage(m);
        }
        garminLaps.add(new GarminLap(lapMesg, lengthMessages));
    }

    public final List<GarminLap> getGarminLaps() {
        return garminLaps;
    }

    public final GarminLap getGarminLap(final int index) {
        return garminLaps.get(index);
    }

    public final void replaceGarminLap(final int index, final GarminLap lap) {
        garminLaps.set(index, lap);
    }

    public final void addRecordMessage(final RecordMesg mesg) {
        Utils.logFitMessage(mesg);
        recordMessages.add(mesg);
    }

    public final List<RecordMesg> getRecordMessages() {
        return recordMessages;
    }

    public final List<RecordMesg> deleteRecordMessages() {
        List<RecordMesg> clonedList = new ArrayList<RecordMesg>(recordMessages);
        recordMessages.clear();
        return clonedList;
    }

    public final void addEventMessage(final EventMesg mesg) {
        Utils.logFitMessage(mesg);
        eventMessages.add(mesg);
    }

    public final List<EventMesg> getEventMessages() {
        return eventMessages;
    }

    public final void addDeviceInfoMessage(final DeviceInfoMesg mesg) {
        Utils.logFitMessage(mesg);
        deviceInfoMessages.add(mesg);
    }

    public final List<DeviceInfoMesg> getDeviceInfoMessages() {
        return deviceInfoMessages;
    }

    public final void addHrMessage(final HrMesg mesg) {
        Utils.logFitMessage(mesg);
        hrMessages.add(mesg);
    }

    public final List<HrMesg> getHrMessages() {
        return hrMessages;
    }

    public final Sport getSport() {
        return sessionMesg.getSport();
    }

    public final SubSport getSubSport() {
        return sessionMesg.getSubSport();
    }

    public final String getDeviceManufacturer() {
        return Manufacturer.getStringFromValue(fileIdMesg.getManufacturer());
    }

    public final String getDeviceName() {
        if (fileIdMesg.getManufacturer() == Manufacturer.GARMIN) {
            return GarminProduct.getStringFromValue(fileIdMesg.getProduct());
        }
        return "UNKONWN";
    }
}
