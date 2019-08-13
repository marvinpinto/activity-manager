package ca.disjoint.fitcustomizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class TestUtils {
    public static String matchRegexGroup(String regex, String stringToMatchAgainst) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringToMatchAgainst);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).trim();
    }

    public static boolean doesRegexPatternMatch(String regex, String stringToMatchAgainst) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stringToMatchAgainst);
        return matcher.matches();
    }
}
