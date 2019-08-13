import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import picocli.CommandLine;

import org.jline.utils.AttributedString;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.nio.charset.StandardCharsets;

import ca.disjoint.fitcustomizer.SwimEditor;
import ca.disjoint.fitcustomizer.TestUtils;

public class SwimEditorTest {
    private SwimEditor inst;
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

    private Terminal getCustomizedTerminal(InputStream inContent, OutputStream outContent) {
        Terminal t = null;
        try {
            t = new DumbTerminal("terminal", "ansi", inContent, outContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return t;
    }

    @After
    public void tearDown() {
        inst = null;
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
        terminal = null;
    }

    @Test
    public void shouldFailIfNoFitFileProvided() {
        String[] args = {};
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required parameter: FILE"));
    }

    @Test
    public void shouldProcessBasicSwimData() {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--verbose", url.getFile() };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        boolean matches = TestUtils.doesRegexPatternMatch(".*Sport:.*Swimming..LapSwimming..*", plainOutput);
        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertTrue("Output did not contain Sport: Swimming (LapSwimming) - output: " + plainOutput, matches);
    }

    @Test
    public void shouldFailIfInvalidArgSupplied() {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile(), "--asdfasdfasdfasf" };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Unknown option: '--asdfasdfasdfasf'"));
    }

    @Test
    public void shouldFailWithCorrectExitCodeWhenProgramFails() {
        String[] args = { "FILEDOESNOTEXIST" };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.SOFTWARE));
        assertThat(errContent.toString(), startsWith("Error: FILEDOESNOTEXIST (No such file or directory)"));
    }

    @Test
    public void shouldPrintVersionWhenRequested() {
        String[] args = { "--version" };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(outContent.toString(), startsWith("SwimEditor.jar v"));
    }

    @Test
    public void shouldRandomizeCreationTimeByDefault() {
        String actualCreationTime = "Wed Jul 04 07:40:39 EDT 2018";
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile() };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();
        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        String capturedDate = TestUtils.matchRegexGroup(".*Date:(.*)", plainOutput);

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(capturedDate, startsWith("Wed Jul 04 07:"));
        assertThat(capturedDate, not(equalTo(actualCreationTime)));
    }

    @Test
    public void shouldEditPoolLength() throws IOException, InterruptedException {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--verbose", "--edit", url.getFile() };

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream();
        pout.connect(pin);
        terminal = getCustomizedTerminal(pin, outContent);

        Thread th = new Thread() {
            public void run() {
                try {
                    Thread.sleep(10);
                    // Backspace to erase the preset pool length value (erases "15.24")
                    // Enter in an invalid value (triggers an error + re-prompt)
                    // Backspace to erase the preset pool length value (erases "15.24")
                    // Enter in a new pool length of 50m
                    pout.write("\b\b\b\b\binvalidlength\n\b\b\b\b\b50\n".getBytes());
                    pout.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();

        inst = new SwimEditor(pin, outContent, terminal, args);
        int exitCode = inst.start();
        th.join();

        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        String capturedPoolLength = TestUtils.matchRegexGroup(".*Pool length:(.*)", plainOutput);
        assertThat(capturedPoolLength, equalTo("50.0m"));
    }
}
