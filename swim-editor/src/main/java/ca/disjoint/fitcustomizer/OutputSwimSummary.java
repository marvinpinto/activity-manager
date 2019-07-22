package ca.disjoint.fitcustomizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

import com.garmin.fit.Fit;
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
import com.garmin.fit.LengthType;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.HrMesg;
import com.garmin.fit.HrMesgListener;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.BufferEncoder;

public class OutputSwimSummary {
    private static final Logger LOGGER = LogManager.getLogger("SwimEditor");
    private DataReader reader;

    public OutputSwimSummary(File fitFile) throws FileNotFoundException, IOException {
        FileInputStream in;
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        reader = new DataReader();

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
        mesgBroadcaster.addListener((HrMesgListener) reader);
        mesgBroadcaster.addListener((RecordMesgListener) reader);

        LOGGER.log(Level.DEBUG, "Decoding FIT file");
        boolean status = decode.read(in, mesgBroadcaster, mesgBroadcaster);
        LOGGER.log(Level.DEBUG, "FIT file decoding complete, status: " + status);

        in.close();
        LOGGER.log(Level.DEBUG, "FIT file handle successfully closed");
    }

    public String getSummaryData() {
        return reader.getSummaryData();
    }

    private static class DataReader
            implements FileIdMesgListener, LapMesgListener, LengthMesgListener, SessionMesgListener,
            ActivityMesgListener, EventMesgListener, DeviceInfoMesgListener, HrMesgListener, RecordMesgListener {
        private StringBuilder summaryData;
        private float movingTime = 0;
        private BufferEncoder updatedFitFile;

        public DataReader() {
            summaryData = new StringBuilder();
            updatedFitFile = new BufferEncoder(Fit.ProtocolVersion.V2_0);
        }

        public String getSummaryData() {
            return summaryData.toString();
        }

        public byte[] getUpdatedFitFile() {
            return updatedFitFile.close();
        }

        @Override
        public void onMesg(RecordMesg mesg) {
            // Write out the record message as-is to the updated FIT file
            updatedFitFile.write(mesg);

            if (mesg.getHeartRate() != null) {
                LOGGER.log(Level.DEBUG, "**************************");
                LOGGER.log(Level.DEBUG, "Record Message:");
                LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
                LOGGER.log(Level.DEBUG, "  HR: " + mesg.getHeartRate());
                LOGGER.log(Level.DEBUG, "  Distance: " + mesg.getDistance());
                LOGGER.log(Level.DEBUG, "**************************");
            }
        }

        @Override
        public void onMesg(HrMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "HR Message:");
            LOGGER.log(Level.DEBUG, "  Number of timestamps: " + mesg.getNumEventTimestamp());
            LOGGER.log(Level.DEBUG, "  Timestamps: " + Arrays.toString(mesg.getEventTimestamp()));
            LOGGER.log(Level.DEBUG, "  Number of filtered BPMs: " + mesg.getNumFilteredBpm());
            LOGGER.log(Level.DEBUG, "  Filtered BPMs: " + Arrays.toString(mesg.getFilteredBpm()));
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(DeviceInfoMesg mesg) {
            // Write out the device info message as-is to the updated FIT file
            updatedFitFile.write(mesg);

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
            // Write out the event message as-is to the updated FIT file
            updatedFitFile.write(mesg);

            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Event Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
        }

        @Override
        public void onMesg(ActivityMesg mesg) {
            summaryData.append(
                    "Timer time: " + convertFloatToStringDate(mesg.getTotalTimerTime()) + System.lineSeparator());

            // Write out the activity message as-is to the updated FIT file
            updatedFitFile.write(mesg);

            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Activity Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event Group: " + mesg.getEventGroup());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Activity type: " + mesg.getType());
            LOGGER.log(Level.DEBUG, "  Local timestamp: " + mesg.getLocalTimestamp());
            LOGGER.log(Level.DEBUG, "  Number of sessions: " + mesg.getNumSessions());
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Total timer time: " + mesg.getTotalTimerTime() + " ("
                    + convertFloatToStringDate(mesg.getTotalTimerTime()) + ")");
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(LapMesg mesg) {
            // Construct the Lap message for the updated FIT file
            LapMesg lapMesg = mesg;
            lapMesg.setTotalDistance(mesg.getTotalDistance());
            updatedFitFile.write(lapMesg);

            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Lap Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event group: " + mesg.getEventGroup());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Pool Lengths: " + mesg.getNumLengths());
            LOGGER.log(Level.DEBUG, "  Distance: " + mesg.getTotalDistance());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(LengthMesg mesg) {
            // Increment the moving time for ACTIVE swim lengths
            if (mesg.getLengthType() == LengthType.ACTIVE) {
                movingTime += mesg.getTotalElapsedTime();
            }

            // Write out the record as-is to the updated FIT file
            updatedFitFile.write(mesg);

            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Length Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Length type: " + mesg.getLengthType());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Elapsed time: " + mesg.getTotalElapsedTime());
            LOGGER.log(Level.DEBUG, "  Strokes/min: " + mesg.getAvgSwimmingCadence());
            LOGGER.log(Level.DEBUG, "  Num of strokes: " + mesg.getTotalStrokes());
            LOGGER.log(Level.DEBUG, "  Stroke: " + mesg.getSwimStroke());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        private String convertFloatToStringDate(float value) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));
            Instant instant = Instant.ofEpochMilli((long) (value * 1000));
            return formatter.format(instant);
        }

        @Override
        public void onMesg(SessionMesg mesg) {
            String elapsedTime = convertFloatToStringDate(mesg.getTotalElapsedTime());

            summaryData.append("Pool length: " + mesg.getPoolLength() + "m" + System.lineSeparator());
            summaryData.append("Lengths swam: " + mesg.getNumActiveLengths() + System.lineSeparator());
            summaryData.append("Number of laps: " + mesg.getNumLaps() + System.lineSeparator());
            summaryData.append(
                    "Total distance: " + String.format("%.0f", mesg.getTotalDistance()) + "m" + System.lineSeparator());
            summaryData.append("Elapsed time: " + elapsedTime + System.lineSeparator());
            summaryData.append("Moving time: " + convertFloatToStringDate(movingTime) + System.lineSeparator());

            // Construct the Session message for the updated FIT file
            SessionMesg sessionMesg = mesg;
            sessionMesg.setPoolLength(mesg.getPoolLength());
            sessionMesg.setPoolLengthUnit(mesg.getPoolLengthUnit());
            sessionMesg.setTotalDistance(mesg.getTotalDistance());
            updatedFitFile.write(sessionMesg);

            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Session Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event group: " + mesg.getEventGroup());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Sport: " + mesg.getSport());
            LOGGER.log(Level.DEBUG, "  Sport index: " + mesg.getSportIndex());
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
            LOGGER.log(Level.DEBUG, "  Pool length unit: " + mesg.getPoolLengthUnit());
            LOGGER.log(Level.DEBUG, "  Total distance: " + mesg.getTotalDistance());
            LOGGER.log(Level.DEBUG, "**************************");
        }

        @Override
        public void onMesg(FileIdMesg mesg) {
            summaryData.append("Device: " + Manufacturer.getStringFromValue(mesg.getManufacturer()) + " "
                    + GarminProduct.getStringFromValue(mesg.getGarminProduct()) + System.lineSeparator());
            summaryData.append("Date: " + mesg.getTimeCreated() + System.lineSeparator());

            // Write out the fileid message as-is to the updated FIT file
            updatedFitFile.write(mesg);

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
