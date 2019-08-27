package ca.disjoint.fit;

import java.util.List;

import com.garmin.fit.LengthMesg;
import com.garmin.fit.LapMesg;

public final class GarminLap {
    private LapMesg lapMessage; // note that one lap has many lengths
    private List<LengthMesg> lengthMessages;

    public GarminLap(final LapMesg lapMessage, final List<LengthMesg> lengthMessages) {
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
