/*
    Activity Manager
    Copyright (C) 2019 - Marvin Pinto

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ca.disjoint.fit;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class TestUtils {
    protected TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static String matchRegexGroup(final String regex, final String stringToMatchAgainst) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringToMatchAgainst);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).trim();
    }

    public static boolean doesRegexPatternMatch(final String regex, final String stringToMatchAgainst) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stringToMatchAgainst);
        return matcher.matches();
    }

    public static void deleteAllTestGeneratedFitFiles() throws IOException {
        // Delete all the remnant *.fit files generated in test mode
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.fit");
        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attribs) {
                Path name = file.getFileName();
                if (matcher.matches(name)) {
                    File f = new File(name.toString());
                    f.delete();
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(
                FileSystems.getDefault().getPath(
                        System.getProperty("java.io.tmpdir") + FileSystems.getDefault().getSeparator() + "maven-tests"),
                matcherVisitor);
    }
}
