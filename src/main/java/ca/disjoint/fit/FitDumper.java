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

import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.File;
import java.util.concurrent.Callable;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import org.jline.terminal.Terminal;

@Command(name = "dump", mixinStandardHelpOptions = true, description = "Dump raw fit data directly to the log file.", usageHelpAutoWidth = true)
public class FitDumper implements Callable<Integer> {
    @Mixin
    private ReusableOptions reusableOptionsMixin;

    @Parameters(arity = "1", paramLabel = "FILE", description = "FIT file to process.")
    private File fitFile;

    @Spec
    private CommandSpec spec;

    private static final Logger LOGGER = LogManager.getLogger(FitDumper.class);

    private Terminal terminal;
    private final InputStream input;
    private final OutputStream output;
    private final String[] cliArgs;
    private GarminGenericActivity garminActivity;

    public FitDumper(final InputStream input, final OutputStream output, final Terminal terminal, final String[] args) {
        this.input = input;
        this.output = output;
        this.cliArgs = args;
        this.terminal = terminal;
    }

    @SuppressWarnings("checkstyle:DesignForExtension")
    public Integer call() {
        Utils.setLogLevel(reusableOptionsMixin);

        if (reusableOptionsMixin.getVerbosity().length <= 0) {
            throw new ParameterException(spec.commandLine(),
                    "Verbose mode needs to be enabled in order for this command to work.");
        }

        try {
            garminActivity = new GarminGenericActivity();
            GarminActivityLoader gal = new GarminActivityLoader(fitFile, garminActivity);
            terminal.writer().append(garminActivity.getActivitySummary());
            terminal.writer().append(System.lineSeparator());
            terminal.flush();
        } catch (Exception ex) {
            String exceptionMsg = ex.getMessage();
            String msg = String.format("Error: %s", ex.getMessage());
            if (exceptionMsg != "") {
                System.err.println(msg);
            }
            LOGGER.log(Level.ERROR, msg);

            if (reusableOptionsMixin.getVerbosity().length > 0) {
                ex.printStackTrace();
            }
            return 1;
        }

        return 0;
    }

    @SuppressWarnings("checkstyle:DesignForExtension")
    public int start() {
        return new CommandLine(this).execute(cliArgs);
    }
}
