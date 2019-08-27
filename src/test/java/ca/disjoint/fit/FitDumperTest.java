import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import picocli.CommandLine;

import org.jline.utils.AttributedString;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import java.nio.charset.StandardCharsets;

import ca.disjoint.fit.FitDumper;
import ca.disjoint.fit.TestUtils;

@SuppressWarnings("checkstyle:MagicNumber")
public class FitDumperTest {
    private FitDumper inst;
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
    public void shouldFailIfNoFitFileProvided() {
        String[] args = {};
        inst = new FitDumper(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required parameter: FILE"));
    }

    @Test
    public void shouldFailIfVerboseModeNotEnabled() {
        URL url = this.getClass().getResource("/1_2700_sample-run.fit");
        String[] args = { url.getFile() };
        inst = new FitDumper(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(),
                startsWith("Verbose mode needs to be enabled in order for this command to work."));
    }

    @Test
    public void shouldFailWithCorrectExitCodeWhenProgramFails() {
        String[] args = { "-v", "FILEDOESNOTEXIST" };
        inst = new FitDumper(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.SOFTWARE));
        assertThat(errContent.toString(), startsWith("Error: FILEDOESNOTEXIST (No such file or directory)"));
    }

    @Test
    public void shouldProcessRunData() {
        URL url = this.getClass().getResource("/1_2700_sample-run.fit");
        String[] args = { "-v", url.getFile() };
        inst = new FitDumper(inContent, outContent, terminal, args);
        int exitCode = inst.start();

        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        assertThat(plainOutput, startsWith("Generic activity loaded successfully, see log file for details."));
    }
}
