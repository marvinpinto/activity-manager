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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public final class GarminGenericActivity extends GarminActivity {
    private static final Logger LOGGER = LogManager.getLogger(GarminGenericActivity.class);

    public GarminGenericActivity() {
        super();
    }

    @Override
    public String getActivitySummary() {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        asb.append("Generic activity loaded successfully, see log file for details.");
        asb.append(System.lineSeparator());
        return asb.toAnsi();
    }
}
