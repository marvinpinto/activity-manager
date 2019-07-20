package ca.disjoint.fitcustomizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

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
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;

public class OutputSwimSummary {
    private static final Logger LOGGER = LogManager.getLogger("SwimEditor");

    public OutputSwimSummary(File fitFile) throws FileNotFoundException, IOException {
        FileInputStream in;
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        DataReader reader = new DataReader();

        LOGGER.log(Level.DEBUG, "Opening input file: " + fitFile.getName());
        in = new FileInputStream(fitFile);

        LOGGER.log(Level.DEBUG, "Checking FIT file integrity");
        boolean fitIntegrityStatus = decode.checkFileIntegrity((InputStream) in);
        if (!fitIntegrityStatus) {
            LOGGER.log(Level.ERROR, "FIT file integrity check failed");
        }
        in.close();
        if (!fitIntegrityStatus) {
            throw new RuntimeException("FIT file integrity check failed");
        }
        LOGGER.log(Level.DEBUG, "FIT file \"" + fitFile.getName() + "\" integrity check successfuly");

        // Re-opening file handle as the head was moved in checkFileIntegrity
        in = new FileInputStream(fitFile);

        LOGGER.log(Level.DEBUG, "Adding event listeners");
        mesgBroadcaster.addListener((FileIdMesgListener) reader);
        mesgBroadcaster.addListener((LapMesgListener) reader);
        mesgBroadcaster.addListener((LengthMesgListener) reader);
        mesgBroadcaster.addListener((SessionMesgListener) reader);
        mesgBroadcaster.addListener((ActivityMesgListener) reader);
        mesgBroadcaster.addListener((EventMesgListener) reader);
        mesgBroadcaster.addListener((DeviceInfoMesgListener) reader);

        LOGGER.log(Level.DEBUG, "Decoding FIT file");
        boolean status = decode.read(in, mesgBroadcaster, mesgBroadcaster);
        LOGGER.log(Level.DEBUG, "FIT file decoding complete, status: " + status);

        in.close();
        LOGGER.log(Level.DEBUG, "FIT file handle successfully closed");
    }

    private static class DataReader implements FileIdMesgListener, LapMesgListener, LengthMesgListener,
            SessionMesgListener, ActivityMesgListener, EventMesgListener, DeviceInfoMesgListener {

        @Override
        public void onMesg(DeviceInfoMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Devce Message:");
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Device index: " + DeviceIndex.getStringFromValue(mesg.getDeviceIndex()));
            LOGGER.log(Level.DEBUG, "  Garmin product: " + GarminProduct.getStringFromValue(mesg.getGarminProduct()));
            LOGGER.log(Level.DEBUG, "  Manufacturer: " + Manufacturer.getStringFromValue(mesg.getManufacturer()));
            LOGGER.log(Level.DEBUG, "  Serial: " + mesg.getSerialNumber());
            LOGGER.log(Level.DEBUG, "  Hardware version: " + mesg.getHardwareVersion());
            LOGGER.log(Level.DEBUG, "  Software version: " + mesg.getSoftwareVersion());
        }

        @Override
        public void onMesg(EventMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Event Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
        }

        @Override
        public void onMesg(ActivityMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Acitivty Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Activity type: " + mesg.getType());
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(LapMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Lap Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Pool Lengths: " + mesg.getNumLengths());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(LengthMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Length Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Length type: " + mesg.getLengthType());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Strokes/min: " + mesg.getAvgSwimmingCadence());
            LOGGER.log(Level.DEBUG, "  Num of strokes: " + mesg.getTotalStrokes());
            LOGGER.log(Level.DEBUG, "  Stroke: " + mesg.getSwimStroke());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(SessionMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Session Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Sport: " + mesg.getSport());
            LOGGER.log(Level.DEBUG, "  Sub Sport: " + mesg.getSubSport());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Elapsed time: " + mesg.getTotalElapsedTime());
            LOGGER.log(Level.DEBUG, "  Moving time: " + mesg.getTotalMovingTime());
            LOGGER.log(Level.DEBUG, "  Timer time: " + mesg.getTotalTimerTime());
            LOGGER.log(Level.DEBUG, "  Standing time: " + mesg.getTimeStanding());
            LOGGER.log(Level.DEBUG, "  Lengths: " + mesg.getNumActiveLengths());
            LOGGER.log(Level.DEBUG, "  Laps: " + mesg.getNumLaps());
            LOGGER.log(Level.DEBUG, "  Average lap time: " + mesg.getAvgLapTime());
            LOGGER.log(Level.DEBUG, "  Average stoke count: " + mesg.getAvgStrokeCount());
            LOGGER.log(Level.DEBUG, "  Pool length: " + mesg.getPoolLength());
            LOGGER.log(Level.DEBUG, "  Total distance: " + mesg.getTotalDistance());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(FileIdMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "File ID Message:");
            LOGGER.log(Level.DEBUG, "  Creation time: " + mesg.getTimeCreated());
            LOGGER.log(Level.DEBUG, "  Garmin product: " + GarminProduct.getStringFromValue(mesg.getGarminProduct()));
            LOGGER.log(Level.DEBUG, "  Manufacturer: " + Manufacturer.getStringFromValue(mesg.getManufacturer()));
            LOGGER.log(Level.DEBUG, "  Number: " + mesg.getNumber());
            LOGGER.log(Level.DEBUG, "  Product: " + mesg.getProduct());
            LOGGER.log(Level.DEBUG, "  Product name: " + mesg.getProductName());
            LOGGER.log(Level.DEBUG, "  Serial: " + mesg.getSerialNumber());
            LOGGER.log(Level.DEBUG, "  Type: " + mesg.getType());
            LOGGER.log(Level.DEBUG, "**************************");
        }
    }
}
