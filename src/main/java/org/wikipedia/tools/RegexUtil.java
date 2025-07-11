// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.Arrays;
import java.util.regex.Pattern;

public class RegexUtil {
    private static final Pattern PROPERTY_ID_PATTERN = Pattern.compile("^P[1-9][0-9]{0,9}$");
    public static final Pattern Q_ID_PATTERN = Pattern.compile("Q[1-9][0-9]{0,9}");

    public static final Pattern INTEGER_PATTERN = Pattern.compile("^[0-9]+$");

    private RegexUtil() {
        // Private constructor to avoid instantiation
    }

    /**
     * Check if value is a valid property Id
     * Note: For values &gt; {@link Integer#MAX_VALUE}, the Mediawiki API will return with an error instead of reporting an entity as missing.
     * @param value id to check
     * @return {@code true} or {@code false}
     */
    public static boolean isValidPropertyId(final String value) {
        return isValidId(PROPERTY_ID_PATTERN, value);
    }

    public static boolean isValidQId(final String value) {
        return isValidId(Q_ID_PATTERN, value);
    }

    private static boolean isValidId(final Pattern pattern, final String value) {
        try {
            if (value != null && pattern.matcher(value).matches()) {
                // Note: For values > {@link Integer#MAX_VALUE}, the Mediawiki API will return with an error instead of reporting an entity as missing.
                Integer.parseInt(value.substring(1));
                return true;
            }
        } catch (NumberFormatException e) {
            // continue
        }
        return false;
    }

    public static boolean isValidMultiQId(final String value) {
        if (value == null || value.startsWith(";") || value.endsWith(";") || value.contains(";;")) {
            return false;
        }
        return Arrays.stream(value.split(";")).allMatch(RegexUtil::isValidQId);
    }

    public static void requireValidQId(final String value) {
        if (!isValidQId(value)) {
            throw new IllegalArgumentException("Q-ID is invalid!");
        }
    }

}
