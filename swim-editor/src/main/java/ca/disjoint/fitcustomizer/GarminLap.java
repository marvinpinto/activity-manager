package ca.disjoint.fitcustomizer;

import java.util.List;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LapMesg;

public class GarminLap {
    private LapMesg lapMessage; // note that one lap has many lengths
    private List<LengthMesg> lengthMessages;

    public GarminLap(LapMesg lapMessage, List<LengthMesg> lengthMessages) {
        this.lapMessage = lapMessage;
        this.lengthMessages = lengthMessages;
    }

    public LapMesg getLapMessage() {
        return lapMessage;
    }

    public List<LengthMesg> getLengthMessages() {
        return lengthMessages;
    }
}
