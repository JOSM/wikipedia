// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openstreetmap.josm.tools.Logging;

public class ApiUrl {
    private ApiUrl() {
        // Private constructor to avoid instantiation
    }

    /**
     * Concatenates all parameters (nulls are treated like empty strings) to a single string and passes that to
     * {@link #url(String)}.
     *
     * @param part1
     *     the first part of the URL
     * @param part2
     *     the second part of the URL
     * @param moreParts
     *     the rest of the parts of the URL
     * @return the URL that is returned from {@link #url(String)}  when passing it the concatenated parts
     * @throws IllegalArgumentException
     *     when the returned URL would be malformed
     */
    public static URL url(final String part1, final String part2, final String... moreParts) {
        return url(
            Stream.concat(Stream.of(part1, part2), Arrays.stream(moreParts))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining())
        );
    }

    /**
     * The same as {@link URL#URL(String)}, except that any {@link MalformedURLException} will be wrapped inside
     * an {@link IllegalArgumentException}, which is unchecked.
     *
     * @param url
     *     the string to parse as URL
     * @return the resulting URL
     * @throws IllegalArgumentException
     *     when the URL parsed from the parameter would be malformed
     *
     */
    public static URL url(final String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            final String message = "The wikipedia plugin tries to construct a malformed URL!";
            Logging.log(Logging.LEVEL_ERROR, message, e);
            throw new IllegalArgumentException(message, e);
        }
    }
}
