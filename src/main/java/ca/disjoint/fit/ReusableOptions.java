package ca.disjoint.fit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command
public class ReusableOptions {
    @Option(names = { "-v", "--verbose" }, description = {
            "Verbose mode, helpful for troubleshooting (\"application.log\" in current directory)",
            "Specify multiple -v options to increase verbosity. (e.g. `-v -v` or `-vv`)" })
    private boolean[] verbosity = new boolean[0];

    @SuppressWarnings("checkstyle:DesignForExtension")
    public boolean[] getVerbosity() {
        return verbosity;
    }
}
