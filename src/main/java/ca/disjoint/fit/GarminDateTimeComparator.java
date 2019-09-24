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
