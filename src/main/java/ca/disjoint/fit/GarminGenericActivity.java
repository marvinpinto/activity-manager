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
