# Garmin Activity Manager

[![GitHub Actions status](https://github.com/marvinpinto/activity-manager/workflows/ci/badge.svg)](https://github.com/marvinpinto/activity-manager/actions)

### Getting Started
- You will need a working JDK environment.

- Accept the [Garmin FIT SDK license](https://www.thisisant.com/resources/fit)
as this project makes use of their SDK.

- Install the fit.jar bundle locally:
  ``` bash
  mvn install:install-file -Dfile=fit-21.14.00.jar -DgroupId=com.garmin.fit -DartifactId=fit-sdk -Dversion=21.14.00 -Dpackaging=jar
  ```

### Development
- `mvn clean package`
- `java -jar target/activity-manager-VERSION-jar-with-dependencies.jar`
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
