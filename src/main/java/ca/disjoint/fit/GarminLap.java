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
