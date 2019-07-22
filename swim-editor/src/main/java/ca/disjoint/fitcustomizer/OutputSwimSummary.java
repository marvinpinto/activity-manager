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
import java.util.Random;

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
import com.garmin.fit.SwimStroke;
import com.garmin.fit.DateTime;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

public class OutputSwimSummary {
    private static final Logger LOGGER = LogManager.getLogger(OutputSwimSummary.class);
    private DataReader reader;
    private TextIO textIO;
    private float poolLength;
    private boolean interactiveEditMode;
    private boolean randomizeCreationTime;

    public OutputSwimSummary(File fitFile, boolean editMode, boolean randomizeStart)
            throws FileNotFoundException, IOException {
        FileInputStream in;
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        textIO = TextIoFactory.getTextIO();
        reader = new DataReader();
        interactiveEditMode = editMode;
        randomizeCreationTime = randomizeStart;

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

        if (interactiveEditMode) {
            // Prompt the user for the pool length
            poolLength = textIO.newFloatInputReader().withDefaultValue(25f).read("Pool Length (m)");
            LOGGER.log(Level.DEBUG, "User entered pool length: " + poolLength + "m");
        }

        LOGGER.log(Level.DEBUG, "Decoding FIT file");
        boolean status = decode.read(in, mesgBroadcaster, mesgBroadcaster);
        LOGGER.log(Level.DEBUG, "FIT file decoding complete, status: " + status);

        in.close();
        LOGGER.log(Level.DEBUG, "FIT file handle successfully closed");
    }

    public String getSummaryData() {
        return reader.getSummaryData();
    }

    private class DataReader implements FileIdMesgListener, LapMesgListener, LengthMesgListener, SessionMesgListener,
            ActivityMesgListener, EventMesgListener, DeviceInfoMesgListener, HrMesgListener, RecordMesgListener {
        private StringBuilder summaryData;
        private StringBuilder lapSummaryData;
        private float movingTime = 0;
        private float totalDistance = 0;
        private BufferEncoder updatedFitFile;
        private int lapCtr = 1;
        private int lengthCtr = 0;
        private SwimStroke lapStroke;

        public DataReader() {
            summaryData = new StringBuilder();
            lapSummaryData = new StringBuilder();
            updatedFitFile = new BufferEncoder(Fit.ProtocolVersion.V2_0);
            lapSummaryData.append("-----------------" + System.lineSeparator());
            lapSummaryData.append("Lap & Length Data" + System.lineSeparator());
            lapSummaryData.append("-----------------" + System.lineSeparator());
            lapStroke = SwimStroke.INVALID;
        }

        public String getSummaryData() {
            summaryData.append(System.lineSeparator());
            summaryData.append(lapSummaryData.toString());
            return summaryData.toString();
        }

        public byte[] getUpdatedFitFile() {
            return updatedFitFile.close();
        }

        @Override
        public void onMesg(RecordMesg mesg) {
            if (mesg.getHeartRate() != null) {
                LOGGER.log(Level.DEBUG, "**************************");
                LOGGER.log(Level.DEBUG, "Record Message:");
                LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
                LOGGER.log(Level.DEBUG, "  HR: " + mesg.getHeartRate());
                LOGGER.log(Level.DEBUG, "  Distance: " + mesg.getDistance());
                LOGGER.log(Level.DEBUG, "**************************");
            }

            // Write out the record message as-is to the updated FIT file
            updatedFitFile.write(mesg);
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
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Devce Message:");
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Device index: " + DeviceIndex.getStringFromValue(mesg.getDeviceIndex()));
            LOGGER.log(Level.DEBUG, "  Garmin product: " + GarminProduct.getStringFromValue(mesg.getGarminProduct()));
            LOGGER.log(Level.DEBUG, "  Manufacturer: " + Manufacturer.getStringFromValue(mesg.getManufacturer()));
            LOGGER.log(Level.DEBUG, "  Serial: " + mesg.getSerialNumber());
            LOGGER.log(Level.DEBUG, "  Hardware version: " + mesg.getHardwareVersion());
            LOGGER.log(Level.DEBUG, "  Software version: " + mesg.getSoftwareVersion());

            // Write out the device info message as-is to the updated FIT file
            updatedFitFile.write(mesg);
        }

        @Override
        public void onMesg(EventMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Event Message:");
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Time: " + mesg.getTimestamp());

            // Write out the event message as-is to the updated FIT file
            updatedFitFile.write(mesg);
        }

        @Override
        public void onMesg(ActivityMesg mesg) {
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

            summaryData.append(
                    "Timer time: " + convertFloatToStringDate(mesg.getTotalTimerTime()) + System.lineSeparator());

            // Write out the activity message as-is to the updated FIT file
            updatedFitFile.write(mesg);
        }

        @Override
        public void onMesg(LapMesg mesg) {
            LOGGER.log(Level.DEBUG, "**************************");
            LOGGER.log(Level.DEBUG, "Lap Message:");
            LOGGER.log(Level.DEBUG, "  Index: " + mesg.getMessageIndex());
            LOGGER.log(Level.DEBUG, "  Event: " + mesg.getEvent());
            LOGGER.log(Level.DEBUG, "  Event group: " + mesg.getEventGroup());
            LOGGER.log(Level.DEBUG, "  Event type: " + mesg.getEventType());
            LOGGER.log(Level.DEBUG, "  Start time: " + mesg.getStartTime());
            LOGGER.log(Level.DEBUG, "  End time: " + mesg.getTimestamp());
            LOGGER.log(Level.DEBUG, "  Elapsed time: " + mesg.getTotalElapsedTime());
            LOGGER.log(Level.DEBUG, "  Pool Lengths: " + mesg.getNumLengths());
            LOGGER.log(Level.DEBUG, "  Distance: " + mesg.getTotalDistance());
            LOGGER.log(Level.DEBUG, "  Stroke: " + mesg.getSwimStroke());
            LOGGER.log(Level.DEBUG, "**************************");

            lapCtr++;
            lengthCtr = 0;

            float distance = mesg.getTotalDistance();
            if (interactiveEditMode) {
                distance = mesg.getNumLengths() * poolLength;
                LOGGER.log(Level.DEBUG, "Updated lap distance from " + mesg.getTotalDistance() + " to " + distance);
            }

            // Increment the total distance
            totalDistance += distance;

            // Append this info to the lap summary data
            if (mesg.getSwimStroke() != null) {
                String summary = String.format("Lap %d: %.0fm (%d lengths, %s, %s)", lapCtr - 1, distance,
                        mesg.getNumLengths(), convertFloatToStringDate(mesg.getTotalElapsedTime()), lapStroke);
                lapSummaryData.append(summary);
                lapSummaryData.append(System.lineSeparator());
                lapSummaryData.append(System.lineSeparator());
            }

            // Construct the Lap message for the updated FIT file
            LapMesg lapMesg = mesg;
            lapMesg.setTotalDistance(distance);
            if (mesg.getSwimStroke() != null) {
                lapMesg.setSwimStroke(lapStroke);

                // Reset the lap stroke
                lapStroke = SwimStroke.INVALID;
            }
            updatedFitFile.write(lapMesg);
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
            LOGGER.log(Level.DEBUG, "  Elapsed time: " + mesg.getTotalElapsedTime());
            LOGGER.log(Level.DEBUG, "  Strokes/min: " + mesg.getAvgSwimmingCadence());
            LOGGER.log(Level.DEBUG, "  Num of strokes: " + mesg.getTotalStrokes());
            LOGGER.log(Level.DEBUG, "  Stroke: " + mesg.getSwimStroke());
            LOGGER.log(Level.DEBUG, "**************************");

            lengthCtr++;

            // Increment the moving time for ACTIVE swim lengths
            if (mesg.getLengthType() == LengthType.ACTIVE) {
                movingTime += mesg.getTotalElapsedTime();
            }

            // Prompt the user to correct the stroke, if desired
            SwimStroke stroke = mesg.getSwimStroke();
            if (stroke != null && interactiveEditMode) {
                String prompt = String.format("Lap %d: Length #%d (%d strokes, %s)", lapCtr, lengthCtr,
                        mesg.getTotalStrokes(), convertFloatToStringDate(mesg.getTotalElapsedTime()));
                stroke = textIO.newEnumInputReader(SwimStroke.class).withDefaultValue(stroke).read(prompt);
            }

            if (stroke != null) {
                // Append this info to the lap summary data
                lapSummaryData.append(String.format("%s: %s (%d strokes)%s", stroke,
                        convertFloatToStringDate(mesg.getTotalElapsedTime()), mesg.getTotalStrokes(),
                        System.lineSeparator()));

                // Set the appropriate "lap" stroke
                if (stroke != lapStroke) {
                    if (lapStroke == SwimStroke.INVALID) {
                        lapStroke = stroke;
                    } else if (lapStroke != SwimStroke.MIXED) {
                        lapStroke = SwimStroke.MIXED;
                    }
                }
            }

            // Construct the Length message for the updated FIT file
            LengthMesg lengthMesg = mesg;
            if (stroke != null) {
                lengthMesg.setSwimStroke(stroke);
            }
            updatedFitFile.write(lengthMesg);
        }

        private String convertFloatToStringDate(float value) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));
            Instant instant = Instant.ofEpochMilli((long) (value * 1000));
            return formatter.format(instant);
        }

        @Override
        public void onMesg(SessionMesg mesg) {
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

            float summaryPoolLength = mesg.getPoolLength();
            if (interactiveEditMode) {
                summaryPoolLength = poolLength;
                LOGGER.log(Level.DEBUG,
                        "Updated summary pool length from " + mesg.getPoolLength() + " to " + summaryPoolLength);
            }

            String elapsedTime = convertFloatToStringDate(mesg.getTotalElapsedTime());

            summaryData.append("Pool length: " + summaryPoolLength + "m" + System.lineSeparator());
            summaryData.append("Lengths swam: " + mesg.getNumActiveLengths() + System.lineSeparator());
            summaryData.append("Number of laps: " + mesg.getNumLaps() + System.lineSeparator());
            summaryData
                    .append("Total distance: " + String.format("%.0f", totalDistance) + "m" + System.lineSeparator());
            summaryData.append("Elapsed time: " + elapsedTime + System.lineSeparator());
            summaryData.append("Moving time: " + convertFloatToStringDate(movingTime) + System.lineSeparator());

            // Construct the Session message for the updated FIT file
            SessionMesg sessionMesg = mesg;
            sessionMesg.setPoolLength(summaryPoolLength);
            sessionMesg.setPoolLengthUnit(mesg.getPoolLengthUnit());
            sessionMesg.setTotalDistance(totalDistance);
            updatedFitFile.write(sessionMesg);
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

            DateTime creationTime = mesg.getTimeCreated();
            if (randomizeCreationTime) {
                // Set the creation time to be 1 < n < 100 seconds in the past
                Random r = new Random();
                long low = 1l;
                long high = 100l;
                long timestamp = (low + (long) (Math.random() * (high - low))) * -1;
                LOGGER.log(Level.DEBUG, "Generate random timestamp value " + timestamp);
                creationTime.add(timestamp);
            }

            summaryData.append("Device: " + Manufacturer.getStringFromValue(mesg.getManufacturer()) + " "
                    + GarminProduct.getStringFromValue(mesg.getGarminProduct()) + System.lineSeparator());
            summaryData.append("Date: " + creationTime + System.lineSeparator());

            // Construct the File ID message for the updated FIT file
            FileIdMesg fileIdMesg = mesg;
            fileIdMesg.setTimeCreated(creationTime);
            updatedFitFile.write(fileIdMesg);
        }
    }
}
