import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import picocli.CommandLine;

import org.jline.utils.AttributedString;

import ca.disjoint.fitcustomizer.SwimEditor;

public class SwimEditorTest {
    private SwimEditor inst;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream terminalOut;

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void tearDown() {
        inst = null;
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void shouldFailIfNoFitFileProvided() {
        String[] args = {};
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Missing required parameter: FILE"));
    }

    @Test
    public void shouldProcessBasicSwimData() {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--verbose", url.getFile() };
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();

        String plainOutput = AttributedString.stripAnsi(outContent.toString());
        Pattern pattern = Pattern.compile(".*Sport:.*Swimming..LapSwimming..*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(plainOutput);

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertTrue("Output did not contain Sport: Swimming (LapSwimming) - output: " + plainOutput, matcher.matches());
    }

    @Test
    public void shouldFailIfInvalidArgSupplied() {
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile(), "--asdfasdfasdfasf" };
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.USAGE));
        assertThat(errContent.toString(), startsWith("Unknown option: '--asdfasdfasdfasf'"));
    }

    @Test
    public void shouldFailWithCorrectExitCodeWhenProgramFails() {
        String[] args = { "FILEDOESNOTEXIST" };
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.SOFTWARE));
        assertThat(errContent.toString(), startsWith("Error: FILEDOESNOTEXIST (No such file or directory)"));
    }

    @Test
    public void shouldPrintVersionWhenRequested() {
        String[] args = { "--version" };
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        assertThat(outContent.toString(), startsWith("SwimEditor.jar v"));
    }

    @Test
    public void shouldRandomizeCreationTimeByDefault() {
        String actualCreationTime = "Wed Jul 04 07:40:39 EDT 2018";
        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile() };
        inst = new SwimEditor(System.in, outContent, args);
        int exitCode = inst.start();
        String plainOutput = AttributedString.stripAnsi(outContent.toString());

        assertThat(exitCode, equalTo(CommandLine.ExitCode.OK));
        Pattern pattern = Pattern.compile(".*Date:(.*)");
        Matcher matcher = pattern.matcher(plainOutput);

        if (!matcher.find()) {
            fail("Output did not appear to contain the creation date. Output: " + plainOutput);
        }

        String capturedDate = matcher.group(1).trim();
        if (!capturedDate.startsWith("Wed Jul 04 07:")) {
            fail("Creation time appears to be invalid: " + capturedDate);
        }

        assertThat(capturedDate, not(equalTo(actualCreationTime)));
    }
}
