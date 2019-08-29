import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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
import java.io.File;

import picocli.CommandLine;

import org.jline.utils.AttributedString;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;

import ca.disjoint.fit.SwimEditor;
import ca.disjoint.fit.TestUtils;

@SuppressWarnings("checkstyle:MagicNumber")
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
    public void shouldNotRandomizeCreationTimeWhenSpecified() {
        String actualCreationTime = "Wed Jul 04 07:40:39 EDT 2018";
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--no-randomize-ctime", url.getFile() };
        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();
        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        String capturedDate = TestUtils.matchRegexGroup(".*Date:(.*)", plainOutput);

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(capturedDate, equalTo(actualCreationTime));
    }

    @Test
    public void shouldEditPoolLength() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
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
                    // Backspace to erase the preset pool length value (erases "22.86")
                    sb.append("\b\b\b\b\b");
                    // Enter in an invalid value (triggers an error + re-prompt)
                    sb.append("invalidlength\n");
                    // Backspace to erase the preset pool length value (erases "22.86")
                    sb.append("\b\b\b\b\b");
                    // Enter in a new pool length of 50m
                    sb.append("50\n");
                    // Enter -1 to simulate ctrl+d, to signal we don't wish to edit any laps
                    sb.append("-1\n");
                    pout.write(sb.toString().getBytes());
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

    @Test
    public void shouldEditSwimmingLaps() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
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
                    // Enter to accept the preset pool length of 22.86
                    sb.append("\n");
                    // Enter an invalid lap number
                    sb.append("5\n");
                    // Edit lap #1
                    sb.append("1\n");
                    // Backspace to erase the preset (current) stroke (erases "breaststroke")
                    sb.append("\b\b\b\b\b\b\b\b\b\b\b\b");
                    // Enter an invalid stroke type
                    sb.append("lambada\n");
                    // Backspace to erase the preset (current) stroke (erases "breaststroke")
                    sb.append("\b\b\b\b\b\b\b\b\b\b\b\b");
                    // Enter "freestyle" as the stroke
                    sb.append("freestyle\n");
                    // Hit "enter" 5 times to accept the default (current) strokes
                    sb.append("\n\n\n\n\n");
                    // Enter -1 to simulate ctrl+d, to signal we don't wish to edit any more laps
                    sb.append("-1\n");
                    pout.write(sb.toString().getBytes());
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
        boolean matches = TestUtils.doesRegexPatternMatch(".*Strokes:.*FR,BR,BR,BR,BR,BR.*", plainOutput);
        assertTrue("Output did not contain \"Strokes: FR,BR,BR,BR,BR,BR\" - output: " + plainOutput, matches);
    }

    @Test
    public void shouldCreateUpdatedFitFile() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--no-randomize-ctime", "--verbose", "--edit", url.getFile() };

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream();
        pout.connect(pin);
        terminal = getCustomizedTerminal(pin, outContent);

        Thread th = new Thread() {
            public void run() {
                try {
                    Thread.sleep(10);
                    // Enter to accept the preset pool length of 22.86
                    sb.append("\n");
                    // Enter -1 to simulate ctrl+d, to signal we don't wish to edit any more laps
                    sb.append("-1\n");
                    pout.write(sb.toString().getBytes());
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

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        String filepath = System.getProperty("java.io.tmpdir") + FileSystems.getDefault().getSeparator()
                + "maven-tests/basic-swim-899638839.fit";
        File updatedFitFile = new File(filepath);
        assertTrue("Updated fit file " + filepath + " did not get created", updatedFitFile.exists());
    }

    @Test
    public void shouldNotCreateUpdatedFitFile() throws IOException, InterruptedException {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--no-randomize-ctime", "--verbose", url.getFile() };

        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();
        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));

        String filepath = System.getProperty("java.io.tmpdir") + FileSystems.getDefault().getSeparator()
                + "maven-tests/basic-swim-899638839.fit";
        File updatedFitFile = new File(filepath);
        assertFalse("Updated fit file " + filepath + " incorrectly got created", updatedFitFile.exists());
    }

    @Test
    public void shouldAddHrDataToSwimActivityWithoutEditing() throws IOException, InterruptedException {
        URL swimActivity = this.getClass().getResource("/1_2700_20190621-swim.fit");
        URL hrActivity = this.getClass().getResource("/1_2347_20190621-hr.fit");
        String[] args = { "--no-randomize-ctime", "--verbose", "--hr-data", hrActivity.getFile(),
                swimActivity.getFile() };

        inst = new SwimEditor(inContent, outContent, terminal, args);
        int exitCode = inst.start();
        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));

        String filepath = System.getProperty("java.io.tmpdir") + FileSystems.getDefault().getSeparator()
                + "maven-tests/1_2700_20190621-swim-930049297.fit";
        File updatedFitFile = new File(filepath);
        assertTrue("Updated fit file " + filepath + " did not get created", updatedFitFile.exists());
    }

    @Test
    public void shouldAddHrDataToSwimActivity() throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        URL swimActivity = this.getClass().getResource("/1_2700_20190621-swim.fit");
        URL hrActivity = this.getClass().getResource("/1_2347_20190621-hr.fit");
        String[] args = { "--no-randomize-ctime", "--edit", "--verbose", "--hr-data", hrActivity.getFile(),
                swimActivity.getFile() };

        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream();
        pout.connect(pin);
        terminal = getCustomizedTerminal(pin, outContent);

        Thread th = new Thread() {
            public void run() {
                try {
                    Thread.sleep(10);
                    // Enter to accept the preset pool length
                    sb.append("\n");
                    // Enter -1 to simulate ctrl+d, to signal we don't wish to edit any more laps
                    sb.append("-1\n");
                    pout.write(sb.toString().getBytes());
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
        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));

        String filepath = System.getProperty("java.io.tmpdir") + FileSystems.getDefault().getSeparator()
                + "maven-tests/1_2700_20190621-swim-930049297.fit";
        File updatedFitFile = new File(filepath);
        assertTrue("Updated fit file " + filepath + " did not get created", updatedFitFile.exists());
    }
}
