# Garmin Activity Manager

### Getting Started
- You will need a working JDK environment.

- Download the [Garmin FIT SDK](https://www.thisisant.com/resources/fit), and
extract the `java/fit.jar` file into the root (development) directory. This
will be used by the maven build as well as during runtime. Note that I cannot
bundle the `fit.jar` file into this project as the Garmin license prohibits me
from doing so.

### Development
- `mvn clean package`
- `java -cp fit.jar:target/activity-manager-VERSION-jar-with-dependencies.jar ca.disjoint.fit.ActivityManager`
- `mvn formatter:format`
- `mvn test`
- `mvn clean verify`
- `mvn -Dmaven.test.skip=true clean package`

### Creating a new release
- Do this work on master: `git checkout master`
- Update the version string in `pom.xml` and commit the changes.
- Create a new version tag: `git tag v1.2.3`
- Update the remote master branch: `git push origin master:master`
- Update GitHub with all the local tag(s): `git push --tags`

### Tools
- View the contents of a file within the jar using: `unzip -q -c myarchive.jar path/to/file`
- List all the files in a jar using: `jar -tf myarchive.jar`
