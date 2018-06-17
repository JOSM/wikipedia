// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;


import org.openstreetmap.josm.data.preferences.DoubleProperty;

public final class WikiProperties {

    public static final DoubleProperty WIKI_LAYER_MARKER_HEIGHT = new DoubleProperty("wikipedia.layer.marker_height", 30.0);

    private WikiProperties() {
        // Private constructor to avoid instantiation
    }
}
