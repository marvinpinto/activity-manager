package ca.disjoint.fitcustomizer;

import com.garmin.fit.*;
import com.garmin.fit.plugins.*;
import com.garmin.fit.csv.MesgCSVWriter;

import java.io.FileInputStream;
import java.io.InputStream;
import ca.disjoint.fitcustomizer.Config;

/**
 * Hello world!
 *
 */
public class SwimEditor implements RecordMesgListener, HrMesgListener {
  private MesgCSVWriter mesgWriter;

  public static void main(String[] args) {
    Config conf = new Config();

    System.out.printf("VERSION: %s\n", conf.VERSION);

    System.out.printf("FIT Hr Record Reader Example Application - Protocol %d.%d Profile %.2f %s\n", Fit.PROTOCOL_VERSION_MAJOR, Fit.PROTOCOL_VERSION_MINOR, Fit.PROFILE_VERSION / 100.0, Fit.PROFILE_TYPE);
  }

  public void onMesg(RecordMesg mesg) {
    mesgWriter.onMesg(mesg);
  }

  public void onMesg(HrMesg mesg) {
    mesgWriter.onMesg(mesg);
  }
}
