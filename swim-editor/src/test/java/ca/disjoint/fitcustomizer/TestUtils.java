package ca.disjoint.fitcustomizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
}
