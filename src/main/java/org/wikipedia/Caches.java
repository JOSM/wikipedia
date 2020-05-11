// License: GPL. For details, see LICENSE file.
package org.wikipedia;

import java.io.File;
import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.spi.preferences.Config;

public final class Caches {
    public static final CacheAccess<String, String> API_RESPONSES = JCSCacheManager.getCache(
        "api",
        1,
        10_000,
        new File(Config.getDirs().getCacheDirectory(false), "plugin/wikipedia").getAbsolutePath()
    );

    private Caches() {
        // Private constructor to avoid instantiation
    }
}
