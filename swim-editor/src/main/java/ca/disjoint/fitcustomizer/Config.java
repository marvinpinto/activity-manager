package ca.disjoint.fitcustomizer;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class Config {
  private Properties props = new Properties();
  public String VERSION;

  public Config() {
    InputStream in = this.getClass().getResourceAsStream("/META-INF/application.properties");
    try {
      props.load(in);
    } catch (IOException e) {
      e.printStackTrace();
    }

    VERSION = props.getProperty("application.version");
  }
}
