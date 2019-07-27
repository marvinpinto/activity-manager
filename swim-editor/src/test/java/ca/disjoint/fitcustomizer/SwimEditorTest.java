import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.Assertion;

import ca.disjoint.fitcustomizer.SwimEditor;

public class SwimEditorTest {
    private SwimEditor inst;

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setUp() {
        inst = new SwimEditor();
    }

    @After
    public void tearDown() {
        inst = null;
    }

    @Test
    public void shouldFailIfNoFitFileProvided() {
        exit.expectSystemExitWithStatus(2);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                assertTrue(systemErrRule.getLog().startsWith("Missing required parameter: FILE"));
            }
        });

        String[] args = {};
        inst.main(args);
    }

    @Test
    public void shouldProcessBasicSwimData() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                assertTrue(systemOutRule.getLog().startsWith("Sport: "));
            }
        });

        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { "--verbose", url.getFile() };
        inst.main(args);
    }

    @Test
    public void shouldFailIfInvalidArgSupplied() {
        exit.expectSystemExitWithStatus(2);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                assertTrue(systemErrRule.getLog().startsWith("Unknown option: '--asdfasdfasdfasf'"));
            }
        });

        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile(), "--asdfasdfasdfasf" };
        inst.main(args);
    }

    @Test
    public void shouldFailWithCorrectExitCodeWhenProgramFails() {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                assertTrue(systemErrRule.getLog().startsWith("Error: FILEDOESNOTEXIST (No such file or directory)"));
            }
        });

        String[] args = { "FILEDOESNOTEXIST" };
        inst.main(args);
    }

    @Test
    public void shouldPrintVersionWhenRequested() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                assertTrue(systemOutRule.getLog().startsWith("SwimEditor.jar v"));
            }
        });

        String[] args = { "--version" };
        inst.main(args);
    }

    @Test
    public void shouldRandomizeCreationTimeByDefault() {
        String actualCreationTime = "Wed Jul 04 07:40:39 EDT 2018";

        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            public void checkAssertion() {
                Pattern pattern = Pattern.compile(".*Date:(.*)\\n.*");
                Matcher matcher = pattern.matcher(systemOutRule.getLog());

                if (!matcher.find()) {
                    fail("Output did not appear to contain the creation date. Output: " + systemOutRule.getLog());
                }

                String capturedDate = matcher.group(1).trim();
                if (!capturedDate.startsWith("Wed Jul 04 07:")) {
                    fail("Creation time appears to be invalid: " + capturedDate);
                }

                assertThat(capturedDate, not(equalTo(actualCreationTime)));
            }
        });

        URL url = this.getClass().getResource("/basic-swim.fit");
        String[] args = { url.getFile() };
        inst.main(args);
    }
}
