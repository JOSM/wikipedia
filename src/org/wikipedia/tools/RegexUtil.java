// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.regex.Pattern;

public class RegexUtil {
    private static final Pattern Q_ID_PATTERN = Pattern.compile("^Q[1-9][0-9]*$");
    private static final Pattern SITE_ID_PATTERN = Pattern.compile("^[a-z][a-z][a-z]?wiki");

    private RegexUtil() {
        // Private constructor to avoid instantiation
    }

    public static boolean isValidQId(final String value) {
        return value != null && Q_ID_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a given string matches "[a-z]{2,3}wiki".
     * This has to be improved in the future to exactly allow only existing site IDs and allow other wikimedia sites
     * other than wikipedias, but for now it's good enough.
     * @param value the potential site ID to check
     * @return {@code true} if the site ID is valid, {@code false} otherwise
     */
    public static boolean isValidSiteId(final String value) {
        return value != null && SITE_ID_PATTERN.matcher(value).matches();
    }
}
