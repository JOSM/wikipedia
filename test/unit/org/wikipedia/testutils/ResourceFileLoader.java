// License: GPL. For details, see LICENSE file.
package org.wikipedia.testutils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class ResourceFileLoader {

    private ResourceFileLoader() {
        // Private constructor to avoid instantiation
    }

    public static byte[] getResourceBytes(final Class<?> relativeToClass, final String path) throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(relativeToClass.getResource(path).toURI()));
    }
}
