package ca.disjoint.fit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.garmin.fit.Fit;
import com.garmin.fit.BufferEncoder;
import com.garmin.fit.Mesg;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.EventMesg;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.RecordMesg;

public final class FitWriter {
    private static final Logger LOGGER = LogManager.getLogger(FitWriter.class);
    private GarminActivity garminActivity;
    private String originalFileName;

    public FitWriter(final GarminActivity garminActivity, final String originalFileName) {
        this.garminActivity = garminActivity;
        this.originalFileName = originalFileName;
    }

    public String writeFitFile() throws FileNotFoundException, IOException {
        BufferEncoder fitFile = new BufferEncoder(Fit.ProtocolVersion.V2_0);
        List<Mesg> sortedMsgList = new ArrayList<Mesg>();
        String newFilename = getNewFileName();

        // File ID message
        fitFile.write(garminActivity.getFileIdMesg());

        // All the event messages
        for (EventMesg msg : garminActivity.getEventMessages()) {
            sortedMsgList.add(msg);
        }

        // All the device info messages
        for (DeviceInfoMesg msg : garminActivity.getDeviceInfoMessages()) {
            sortedMsgList.add(msg);
        }

        // All the record messages
        for (RecordMesg msg : garminActivity.getRecordMessages()) {
            sortedMsgList.add(msg);
        }

        // All the individual laps and their corresponding lengths
        List<GarminLap> garminLaps = garminActivity.getGarminLaps();
        for (GarminLap lap : garminLaps) {
            for (LengthMesg length : lap.getLengthMessages()) {
                sortedMsgList.add(length);
            }
            sortedMsgList.add(lap.getLapMessage());
        }

        // Activity message
        sortedMsgList.add(garminActivity.getActivityMesg());

        // Session message
        sortedMsgList.add(garminActivity.getSessionMesg());

        // Sort all the messages according to their timestamp
        sortedMsgList.sort(new GarminDateTimeComparator<Mesg>());

        LOGGER.log(Level.DEBUG, "=========== Debug logging sorted messages ===========");
        for (Mesg m : sortedMsgList) {
            Utils.logFitMessage(m);
            fitFile.write(m);
        }

        byte[] rawBytes = fitFile.close();
        OutputStream os = new FileOutputStream(newFilename);
        LOGGER.log(Level.DEBUG, "Writing updated FIT contents out to file " + newFilename);
        os.write(rawBytes);
        os.close();

        boolean fitIntegrityStatus = Utils.checkFitFileIntegrity(newFilename);
        if (!fitIntegrityStatus) {
            throw new RuntimeException("FIT file integrity check failed");
        }

        return newFilename;
    }

    private String getNewFileName() {
        LOGGER.log(Level.DEBUG, "Determining file name for update FIT file (original: " + originalFileName + ")");

        String[] tokens = originalFileName.split("\\.(?=[^\\.]+$)");
        String base = tokens[0];
        String ext = tokens[1];
        String newFileName = base + "-" + garminActivity.getCreationTime().getTimestamp() + "." + ext;
        LOGGER.log(Level.DEBUG, "Checking to see if filename \"" + newFileName + "\" is useable");

        File newFile = new File(newFileName);
        if (newFile.exists()) {
            throw new RuntimeException("File \"" + newFileName + "\" already exists and will not be overwritten");
        }

        LOGGER.log(Level.DEBUG, "Filename \"" + newFileName + "\" appears to be useable");
        return newFileName;
    }
}
