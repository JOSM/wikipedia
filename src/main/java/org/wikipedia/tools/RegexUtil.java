// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.regex.Pattern;

public class RegexUtil {
    private static final Pattern PROPERTY_ID_PATTERN = Pattern.compile("^P[1-9][0-9]*$");
    private static final Pattern Q_ID_PATTERN = Pattern.compile("^Q[1-9][0-9]*$");
    private static final Pattern MULTI_Q_ID_PATTERN = Pattern.compile("^Q[1-9][0-9]*(;Q[1-9][0-9]*)*$");
    private static final Pattern SITE_ID_PATTERN = Pattern.compile("^[a-z][a-z][a-z]?wiki$");
    public static final Pattern WIKIPEDIA_TAG_VALUE_PATTERN = Pattern.compile("([a-z][a-z][a-z]?):(.+)");

    public static final Pattern INTEGER_PATTERN = Pattern.compile("^[0-9]+$");

    private RegexUtil() {
        // Private constructor to avoid instantiation
    }

    public static boolean isValidPropertyId(final String value) {
        return value != null && PROPERTY_ID_PATTERN.matcher(value).matches();
    }

    public static boolean isValidQId(final String value) {
        return value != null && Q_ID_PATTERN.matcher(value).matches();
    }

    public static boolean isValidMultiQId(final String value) {
        return value != null && MULTI_Q_ID_PATTERN.matcher(value).matches();
    }

    public static void requireValidQId(final String value) {
        if (!isValidQId(value)) {
            throw new IllegalArgumentException("Q-ID is invalid!");
        }
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

    public static boolean isValidWikipediaTagValue(final String value) {
        return value != null && WIKIPEDIA_TAG_VALUE_PATTERN.matcher(value).matches();
    }
}
