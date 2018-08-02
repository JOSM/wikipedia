// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.Arrays;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.LanguageInfo;

public final class WikiProperties {

    public static final DoubleProperty WIKI_LAYER_MARKER_HEIGHT = new DoubleProperty("wikipedia.layer.marker_height", 30.0);

    private static final String JOSM_LOCALE = LanguageInfo.getJOSMLocaleCode();

    public static final StringProperty WIKIPEDIA_LANGUAGE = new StringProperty(
        "wikipedia.lang",
        JOSM_LOCALE.substring(0, JOSM_LOCALE.indexOf('_') >= 1 ? JOSM_LOCALE.indexOf('_') : JOSM_LOCALE.length())
    );

    public static final ListProperty WIKIDATA_VALIDATOR_UNUSUAL_CLASSES = new ListProperty(
        "wikipedia.validator.wikidata.unusual-classes",
        Arrays.asList(
            "Q36774", /* web page (includes e.g. disambiguation pages) */
            "Q215627", /* person */
            "Q729", /* animal */
            "Q8253", /* fiction */
            "Q16521", /* taxon */
            "Q167270", /* brand */
            "Q507619", /* chain store */
            "Q12139612", /* list */
            "Q732577" /* publication */
        )
    );

    private WikiProperties() {
        // Private constructor to avoid instantiation
    }
}
