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

import com.garmin.fit.SwimStroke;

public enum GarminSwimStroke {
    BK(SwimStroke.BACKSTROKE.getValue()), BR(SwimStroke.BREASTSTROKE.getValue()), FL(
            SwimStroke.BUTTERFLY.getValue()), DR(SwimStroke.DRILL.getValue()), FR(SwimStroke.FREESTYLE.getValue()), IM(
                    SwimStroke.IM.getValue()), INV(SwimStroke.INVALID.getValue()), MIX(SwimStroke.MIXED.getValue());

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public short value;

    GarminSwimStroke(final short value) {
        this.value = value;
    }

    public static GarminSwimStroke getByValue(final Short value) {
        for (final GarminSwimStroke type : GarminSwimStroke.values()) {
            if (value == type.value) {
                return type;
            }
        }

        return GarminSwimStroke.INV;
    }
}
