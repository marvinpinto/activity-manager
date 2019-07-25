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

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import ca.disjoint.fitcustomizer.Utils;

public class GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminActivity.class);
    private FileIdMesg fileIdMesg;
    private ActivityMesg activityMesg;
    private SessionMesg sessionMesg;
    private float swimmingPoolLength = 0f;
    private Map<LapMesg, List<LengthMesg>> garminLaps; // one lap has many lengths
    private List<RecordMesg> recordMessages;
    private List<EventMesg> eventMessages;
    private List<DeviceInfoMesg> deviceInfoMessages;
    private List<HrvMesg> hrvMessages;

    public GarminActivity() {
        garminLaps = new HashMap<LapMesg, List<LengthMesg>>();
        recordMessages = new ArrayList<RecordMesg>();
        eventMessages = new ArrayList<EventMesg>();
        deviceInfoMessages = new ArrayList<DeviceInfoMesg>();
        hrvMessages = new ArrayList<HrvMesg>();
    }

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

    public void updateSwimmingPoolLength(float length) {
        swimmingPoolLength = length;
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
        garminLaps.put(lapMesg, lengthMessages);
    }

    public Map<LapMesg, List<LengthMesg>> getGarminLaps() {
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
}
