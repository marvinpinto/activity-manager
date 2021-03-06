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
import com.garmin.fit.HrMesg;
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
        LOGGER.log(Level.DEBUG, "=========== Debug logging sorted messages complete ===========");

        LOGGER.log(Level.DEBUG, "Writing HrMesg entries to the FIT file");
        for (HrMesg m : garminActivity.getHrMessages()) {
            HrMesg newMsg = new HrMesg();
            if (m.getTimestamp() != null) {
                newMsg.setTimestamp(m.getTimestamp());
            }
            for (int i = 0; i < m.getNumEventTimestamp(); i++) {
                newMsg.setEventTimestamp(i, m.getEventTimestamp(i));
            }
            if (m.getFractionalTimestamp() != null) {
                newMsg.setFractionalTimestamp(m.getFractionalTimestamp());
            }
            for (int i = 0; i < m.getNumFilteredBpm(); i++) {
                newMsg.setFilteredBpm(i, m.getFilteredBpm(i));
            }

            // Avoid writing out the event_timestamp_12 entries (for now) as
            // they seem to be causing overflow issues with the other `hr`
            // fields. Uncomment the block below and run the `FitWriter` tests
            // for clarification.
            // for (int i=0;i<m.getNumEventTimestamp12(); i++) {
            // newMsg.setEventTimestamp12(i, m.getEventTimestamp12(i));
            // }

            Utils.logFitMessage(newMsg);
            fitFile.write(newMsg);
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
        String newFileName = base + "-" + garminActivity.getCreationTime().getTimestamp() + ".fit";
        LOGGER.log(Level.DEBUG, "Checking to see if filename \"" + newFileName + "\" is useable");

        File newFile = new File(newFileName);
        if (newFile.exists()) {
            throw new RuntimeException("File \"" + newFileName + "\" already exists and will not be overwritten");
        }

        LOGGER.log(Level.DEBUG, "Filename \"" + newFileName + "\" appears to be useable");
        return newFileName;
    }
}
