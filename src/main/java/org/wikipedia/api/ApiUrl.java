// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import java.net.MalformedURLException;
import java.net.URL;
import org.openstreetmap.josm.tools.Logging;

public class ApiUrl {
    private ApiUrl() {
        // Private constructor to avoid instantiation
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
            final String message = String.format("The wikipedia plugin tries to construct a malformed URL!: %s", url);
            Logging.log(Logging.LEVEL_ERROR, message, e);
            throw new IllegalArgumentException(message, e);
        }
    }
}
