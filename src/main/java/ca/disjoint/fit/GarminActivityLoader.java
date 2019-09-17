package ca.disjoint.fit;

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
import com.garmin.fit.HrMesg;
import com.garmin.fit.HrMesgListener;

public class GarminActivityLoader {
    private static final Logger LOGGER = LogManager.getLogger(GarminActivityLoader.class);
    private DataReader reader;
    private List<LengthMesg> lengthMessages;
    private GarminActivity garminActivity;

    public GarminActivityLoader(final File inputFile, final GarminActivity garminActivity)
            throws FileNotFoundException, IOException {
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        reader = new DataReader();
        this.garminActivity = garminActivity;
        lengthMessages = new ArrayList<LengthMesg>();
        File fitFile = Utils.extractZippedFitFile(inputFile);

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
        mesgBroadcaster.addListener((HrMesgListener) reader);

        LOGGER.log(Level.DEBUG, "Decoding FIT file: " + fitFile.getName());
        boolean status = decode.read(in, mesgBroadcaster, mesgBroadcaster);
        LOGGER.log(Level.DEBUG, "FIT file decoding complete for file " + fitFile.getName() + ", status: " + status);

        in.close();
        LOGGER.log(Level.DEBUG, "FIT file handle successfully closed");
    }

    private class DataReader implements FileIdMesgListener, LapMesgListener, LengthMesgListener, SessionMesgListener,
            ActivityMesgListener, EventMesgListener, DeviceInfoMesgListener, RecordMesgListener, HrMesgListener {

        @Override
        public void onMesg(final RecordMesg mesg) {
            garminActivity.addRecordMessage(mesg);
        }

        @Override
        public void onMesg(final DeviceInfoMesg mesg) {
            garminActivity.addDeviceInfoMessage(mesg);
        }

        @Override
        public void onMesg(final EventMesg mesg) {
            garminActivity.addEventMessage(mesg);
        }

        @Override
        public void onMesg(final ActivityMesg mesg) {
            garminActivity.setActivityMesg(mesg);
        }

        @Override
        public void onMesg(final LapMesg mesg) {
            garminActivity.addGarminLap(mesg, lengthMessages);

            // Reset the lengthMessages list for the next lap
            lengthMessages = new ArrayList<LengthMesg>();
        }

        @Override
        public void onMesg(final LengthMesg mesg) {
            lengthMessages.add(mesg);
        }

        @Override
        public void onMesg(final SessionMesg mesg) {
            garminActivity.setSessionMesg(mesg);
        }

        @Override
        public void onMesg(final FileIdMesg mesg) {
            garminActivity.setFileIdMesg(mesg);
        }

        @Override
        public void onMesg(final HrMesg mesg) {
            garminActivity.addHrMessage(mesg);
        }
    }
}
