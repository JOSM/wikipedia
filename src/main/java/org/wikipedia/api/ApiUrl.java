// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import org.openstreetmap.josm.tools.I18n;
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
            // I18n: {0} is the URL that the plugin tries to construct
            final String message = I18n.tr("The wikipedia plugin tries to construct a malformed URL!: {0}", url);
            Logging.log(Logging.LEVEL_ERROR, message, e);
            throw new IllegalArgumentException(message, e);
        }
    }
}
