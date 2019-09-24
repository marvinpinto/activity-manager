
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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import picocli.CommandLine;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.nio.charset.StandardCharsets;

import ca.disjoint.fit.ActivityManager;
import ca.disjoint.fit.TestUtils;

public class ActivityManagerTest {
    private ActivityManager inst;
    private ByteArrayInputStream inContent = null;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private Terminal terminal;

    @Before
    public void setUp() {
        System.setIn(inContent);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        terminal = getCustomizedTerminal(inContent, outContent);
    }

    private Terminal getCustomizedTerminal(final InputStream inContent, final OutputStream outContent) {
        Terminal t = null;
        try {
            t = new DumbTerminal("terminal", "ansi", inContent, outContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return t;
    }

    @After
    public void tearDown() throws IOException {
        inst = null;
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
        terminal = null;
        TestUtils.deleteAllTestGeneratedFitFiles();
    }

    @Test
    public void shouldPrintVersionWhenRequested() {
        String[] args = { "--version" };
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(outContent.toString(), startsWith("ActivityManager v"));
    }

    @Test
    public void shouldFailIfNoSubcommandProvided() {
        String[] args = {};
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required subcommand"));
    }

    @Test
    public void shouldFailIfInvalidArgSupplied() {
        String[] args = { "--asdfasdfasdfasf" };
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Unknown option: '--asdfasdfasdfasf'"));
    }

    @Test
    public void shouldInvokeHelpSubcommand() {
        String[] args = { "help" };
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(outContent.toString(), startsWith("Usage: fit [-hV] COMMAND"));
    }

    @Test
    public void shouldInvokeSwimSubcommand() {
        String[] args = { "swim" };
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required parameter: FILE"));
    }

    @Test
    public void shouldInvokeDumpSubcommand() {
        String[] args = { "dump" };
        inst = new ActivityManager(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required parameter: FILE"));
    }
}
