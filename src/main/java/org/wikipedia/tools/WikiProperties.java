// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;


import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.LanguageInfo;

public final class WikiProperties {

    public static final DoubleProperty WIKI_LAYER_MARKER_HEIGHT = new DoubleProperty("wikipedia.layer.marker_height", 30.0);

    public static final StringProperty WIKIPEDIA_LANGUAGE = new StringProperty("wikipedia.lang", LanguageInfo.getJOSMLocaleCode().substring(0, 2));

    private WikiProperties() {
        // Private constructor to avoid instantiation
    }
}
