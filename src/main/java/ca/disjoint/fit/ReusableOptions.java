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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command
public class ReusableOptions {
    @Option(names = { "-v", "--verbose" }, description = {
            "Verbose mode, helpful for troubleshooting (\"application.log\" in current directory)",
            "Specify multiple -v options to increase verbosity. (e.g. `-v -v` or `-vv`)" })
    private boolean[] verbosity = new boolean[0];

    @SuppressWarnings("checkstyle:DesignForExtension")
    public boolean[] getVerbosity() {
        return verbosity;
    }
}
