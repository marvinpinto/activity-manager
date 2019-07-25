package ca.disjoint.fitcustomizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;

import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.LapMesgListener;
import com.garmin.fit.LengthMesgListener;
import com.garmin.fit.SessionMesgListener;
import com.garmin.fit.ActivityMesgListener;
import com.garmin.fit.EventMesgListener;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.EventMesg;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.RecordMesg;

import ca.disjoint.fitcustomizer.Utils;
import ca.disjoint.fitcustomizer.GarminActivity;

public class SwimSummary {
    private static final Logger LOGGER = LogManager.getLogger(SwimSummary.class);
    private DataReader reader;
    private GarminActivity garminActivity;
    private List<LengthMesg> lengthMessages;

    public SwimSummary(File fitFile) throws FileNotFoundException, IOException {
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        reader = new DataReader();
        garminActivity = new GarminActivity();
        lengthMessages = new ArrayList<LengthMesg>();

        boolean fitIntegrityStatus = Utils.checkFitFileIntegrity(fitFile);
        if (!fitIntegrityStatus) {
            throw new RuntimeException("FIT file integrity check failed");
        }

        FileInputStream in = new FileInputStream(fitFile);

        LOGGER.log(Level.DEBUG, "Adding event listeners");
        mesgBroadcaster.addListener((FileIdMesgListener) reader);
        mesgBroadcaster.addListener((LapMesgListener) reader);
        mesgBroadcaster.addListener((LengthMesgListener) reader);
        mesgBroadcaster.addListener((SessionMesgListener) reader);
        mesgBroadcaster.addListener((ActivityMesgListener) reader);
        mesgBroadcaster.addListener((EventMesgListener) reader);
        mesgBroadcaster.addListener((DeviceInfoMesgListener) reader);
        mesgBroadcaster.addListener((RecordMesgListener) reader);

        LOGGER.log(Level.DEBUG, "Decoding FIT file");
        boolean status = decode.read(in, mesgBroadcaster, mesgBroadcaster);
        LOGGER.log(Level.DEBUG, "FIT file decoding complete, status: " + status);

        in.close();
        LOGGER.log(Level.DEBUG, "FIT file handle successfully closed");
    }

    private class DataReader implements FileIdMesgListener, LapMesgListener, LengthMesgListener, SessionMesgListener,
            ActivityMesgListener, EventMesgListener, DeviceInfoMesgListener, RecordMesgListener {

        @Override
        public void onMesg(RecordMesg mesg) {
            garminActivity.addRecordMessage(mesg);
        }

        @Override
        public void onMesg(DeviceInfoMesg mesg) {
            garminActivity.addDeviceInfoMessage(mesg);
        }

        @Override
        public void onMesg(EventMesg mesg) {
            garminActivity.addEventMessage(mesg);
        }

        @Override
        public void onMesg(ActivityMesg mesg) {
            garminActivity.setActivityMesg(mesg);
        }

        @Override
        public void onMesg(LapMesg mesg) {
            garminActivity.addGarminLap(mesg, lengthMessages);

            // Reset the lengthMessages list for the next lap
            lengthMessages = new ArrayList<LengthMesg>();
        }

        @Override
        public void onMesg(LengthMesg mesg) {
            lengthMessages.add(mesg);
        }

        @Override
        public void onMesg(SessionMesg mesg) {
            garminActivity.setSessionMesg(mesg);
        }

        @Override
        public void onMesg(FileIdMesg mesg) {
            garminActivity.setFileIdMesg(mesg);
        }
    }
}
