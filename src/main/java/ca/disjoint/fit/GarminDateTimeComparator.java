package ca.disjoint.fit;

import java.util.Comparator;

import com.garmin.fit.Mesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.Fit;

public class GarminDateTimeComparator<T extends Mesg> implements Comparator<T> {
    @SuppressWarnings("checkstyle:DesignForExtension")
    public int compare(final T m1, final T m2) {
        DateTime d1 = new DateTime(m1.getFieldLongValue(Fit.FIELD_NUM_TIMESTAMP));
        DateTime d2 = new DateTime(m2.getFieldLongValue(Fit.FIELD_NUM_TIMESTAMP));
        return d1.compareTo(d2);
    }
}
