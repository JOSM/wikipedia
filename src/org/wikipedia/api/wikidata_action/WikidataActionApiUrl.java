// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.util.Collection;

import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.tools.RegexUtil;

public class WikidataActionApiUrl {
    private static final String BASE_URL = "https://www.wikidata.org/w/api.php";

    private WikidataActionApiUrl() {
        // Private constructor to avoid instantiation
    }

    public static URL checkEntityExistsUrl(final Collection<String> qIds) {
        if (qIds.size() < 1) {
            throw new IllegalArgumentException("You must supply at least one Q-ID to construct a checkEntityExists URL.");
        }
        if (!qIds.stream().allMatch(RegexUtil::isValidQId)) {
            throw new IllegalArgumentException("You must supply only Q-IDs as argument to construct a checkEntityExists URL.");
        }
        return ApiUrl.url(
            BASE_URL,
            "?action=wbgetentities&format=json&sites=&props=&ids=",
            Utils.encodeUrl(String.join("|", qIds))
        );
    }

}
