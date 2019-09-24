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

import com.garmin.fit.Fit;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

@Command(name = "fit", mixinStandardHelpOptions = true, versionProvider = ActivityManager.PropertiesVersionProvider.class, description = "Console application to edit Garmin FIT files.", synopsisSubcommandLabel = "COMMAND", subcommands = {
        HelpCommand.class }, usageHelpAutoWidth = true)
public class ActivityManager implements Callable<Integer> {
    private Terminal terminal;
    private final InputStream input;
    private final OutputStream output;
    private final String[] cliArgs;
    private LineReader reader;

    private static final Logger LOGGER = LogManager.getLogger(ActivityManager.class);

    public ActivityManager(final InputStream input, final OutputStream output, final Terminal terminal,
            final String[] args) {
        this.input = input;
        this.output = output;
        this.cliArgs = args;
        this.terminal = terminal;
        reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    @Spec
    private CommandSpec spec;

    @SuppressWarnings("checkstyle:DesignForExtension")
    public Integer call() {
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    public static void main(final String[] args) throws IOException {
        Terminal term = TerminalBuilder.builder().system(true).signalHandler(Terminal.SignalHandler.SIG_IGN).build();
        ActivityManager inst = new ActivityManager(System.in, System.out, term, args);
        int exitCode = inst.start();
        System.exit(exitCode);
    }

    @SuppressWarnings("checkstyle:DesignForExtension")
    public int start() {
        CommandLine cli = new CommandLine(this);
        cli.addSubcommand(new SwimEditor(input, output, terminal, cliArgs));
        cli.addSubcommand(new FitDumper(input, output, terminal, cliArgs));
        return cli.execute(cliArgs);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    static class PropertiesVersionProvider implements IVersionProvider {
        public String[] getVersion() throws Exception {
            URL url = getClass().getResource("/META-INF/application.properties");
            if (url == null) {
                return new String[] { "N/A" };
            }
            Properties properties = new Properties();
            properties.load(url.openStream());
            return new String[] { "ActivityManager v" + properties.getProperty("application.version"),
                    String.format("FIT protocol %d.%d, profile %.2f %s", Fit.PROTOCOL_VERSION_MAJOR,
                            Fit.PROTOCOL_VERSION_MINOR, Fit.PROFILE_VERSION / 100.0, Fit.PROFILE_TYPE) };
        }
    }
}
