# Garmin Fit Customizer

### Getting Started
- You will need a working JDK environment.

- Download the [Garmin FIT SDK](https://www.thisisant.com/resources/fit), and
extract the `java/fit.jar` file into the root (development) directory. This
will be used by the maven build as well as during runtime. Note that I cannot
bundle the `fit.jar` file into this project as the Garmin license prohibits me
from doing so.

### Development
- `mvn clean package`
- `java -cp fit.jar:swim-editor/target/swim-editor-1.0-SNAPSHOT.jar ca.disjoint.fitcustomizer.SwimEditor`
- `mvn formatter:format`

### Tools
- View the contents of a file within the jar using: `unzip -q -c myarchive.jar path/to/file`
- List all the files in a jar using: `jar -tf myarchive.jar`
