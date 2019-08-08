package ca.disjoint.fitcustomizer;

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
import com.garmin.fit.HrvMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;

import ca.disjoint.fitcustomizer.Utils;
import ca.disjoint.fitcustomizer.GarminLap;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public abstract class GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminActivity.class);
    protected FileIdMesg fileIdMesg;
    protected ActivityMesg activityMesg;
    protected SessionMesg sessionMesg;
    protected List<GarminLap> garminLaps; // one lap has many lengths
    protected List<RecordMesg> recordMessages;
    protected List<EventMesg> eventMessages;
    protected List<DeviceInfoMesg> deviceInfoMessages;
    protected List<HrvMesg> hrvMessages;

    public GarminActivity() {
        garminLaps = new ArrayList<GarminLap>();
        recordMessages = new ArrayList<RecordMesg>();
        eventMessages = new ArrayList<EventMesg>();
        deviceInfoMessages = new ArrayList<DeviceInfoMesg>();
        hrvMessages = new ArrayList<HrvMesg>();
    }

    public String getActivitySummaryHeader() {
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

    public void setFileIdMesg(FileIdMesg mesg) {
        Utils.logFitMessage(mesg);
        fileIdMesg = mesg;
    }

    public void randomizeCreationTime() {
        DateTime creationTime = fileIdMesg.getTimeCreated();

        Random r = new Random();
        long low = 1l;
        long high = 100l;
        long timestamp = (low + (long) (Math.random() * (high - low))) * -1;
        LOGGER.log(Level.DEBUG, "Generated random timestamp value " + timestamp);
        creationTime.add(timestamp);
        fileIdMesg.setTimeCreated(creationTime);
    }

    public DateTime getCreationTime() {
        return fileIdMesg.getTimeCreated();
    }

    public FileIdMesg getFileIdMesg() {
        return fileIdMesg;
    }

    public void setActivityMesg(ActivityMesg mesg) {
        Utils.logFitMessage(mesg);
        activityMesg = mesg;
    }

    public ActivityMesg getActivityMesg() {
        return activityMesg;
    }

    public void setSessionMesg(SessionMesg mesg) {
        Utils.logFitMessage(mesg);
        sessionMesg = mesg;
    }

    public SessionMesg getSessionMesg() {
        return sessionMesg;
    }

    public void addGarminLap(LapMesg lapMesg, List<LengthMesg> lengthMessages) {
        Utils.logFitMessage(lapMesg);
        for (LengthMesg m : lengthMessages) {
            Utils.logFitMessage(m);
        }
        garminLaps.add(new GarminLap(lapMesg, lengthMessages));
    }

    public List<GarminLap> getGarminLaps() {
        return garminLaps;
    }

    public void addRecordMessage(RecordMesg mesg) {
        Utils.logFitMessage(mesg);
        recordMessages.add(mesg);
    }

    public List<RecordMesg> getRecordMessages() {
        return recordMessages;
    }

    public void addEventMessage(EventMesg mesg) {
        Utils.logFitMessage(mesg);
        eventMessages.add(mesg);
    }

    public List<EventMesg> getEventMessages() {
        return eventMessages;
    }

    public void addDeviceInfoMessage(DeviceInfoMesg mesg) {
        Utils.logFitMessage(mesg);
        deviceInfoMessages.add(mesg);
    }

    public List<DeviceInfoMesg> getDeviceInfoMessages() {
        return deviceInfoMessages;
    }

    public void addHrvMessage(HrvMesg mesg) {
        Utils.logFitMessage(mesg);
        hrvMessages.add(mesg);
    }

    public List<HrvMesg> getHrvMessages() {
        return hrvMessages;
    }

    public Sport getSport() {
        return sessionMesg.getSport();
    }

    public SubSport getSubSport() {
        return sessionMesg.getSubSport();
    }
}
