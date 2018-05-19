// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import java.net.URL;

public class InvalidApiQueryException extends Exception {
    public InvalidApiQueryException(final URL url) {
        super("The API query to the following URL is invalid: " + url);
    }
}
