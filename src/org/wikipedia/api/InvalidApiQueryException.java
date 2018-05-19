// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import java.net.URL;

import javax.annotation.Nonnull;

public class InvalidApiQueryException extends Exception {
    public InvalidApiQueryException(@Nonnull final URL url) {
        super("The API query to the following URL is invalid: " + url);
    }
}
