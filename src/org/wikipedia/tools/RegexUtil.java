// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.regex.Pattern;

public class RegexUtil {
    private static final Pattern Q_ID_PATTERN = Pattern.compile("^Q[1-9][0-9]*$");

    private RegexUtil() {
        // Private constructor to avoid instantiation
    }

    public static boolean isValidQId(final String value) {
        return value != null && Q_ID_PATTERN.matcher(value).matches();
    }
}
