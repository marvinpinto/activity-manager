package ca.disjoint.fitcustomizer;

import com.garmin.fit.SwimStroke;

public enum GarminSwimStroke {
    BK(SwimStroke.BACKSTROKE.getValue()), BR(SwimStroke.BREASTSTROKE.getValue()), FL(
            SwimStroke.BUTTERFLY.getValue()), DR(SwimStroke.DRILL.getValue()), FR(SwimStroke.FREESTYLE.getValue()), IM(
                    SwimStroke.IM.getValue()), INV(SwimStroke.INVALID.getValue()), MIX(SwimStroke.MIXED.getValue());

    public short value;

    private GarminSwimStroke(short value) {
        this.value = value;
    }

    public static GarminSwimStroke getByValue(final Short value) {
        for (final GarminSwimStroke type : GarminSwimStroke.values()) {
            if (value == type.value)
                return type;
        }

        return GarminSwimStroke.INV;
    }
}
