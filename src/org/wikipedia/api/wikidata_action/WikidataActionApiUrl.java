// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.util.Collection;

import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.tools.RegexUtil;

public class WikidataActionApiUrl {
    private static final String BASE_URL = "https://www.wikidata.org/w/api.php?";
    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";

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
            FORMAT_PARAMS,
            "&action=wbgetentities&sites=&props=&ids=",
            Utils.encodeUrl(String.join("|", qIds))
        );
    }

    public static URL getEntityForSitelink(final String siteId, final Collection<String> titles) {
        if (siteId == null || titles == null || titles.size() <= 0) {
            throw new IllegalArgumentException("The site ID and titles must be present!");
        }
        if (!RegexUtil.isValidSiteId(siteId)) {
            throw new IllegalArgumentException("The site ID is not given in the expected format!");
        }
        return ApiUrl.url(
            BASE_URL,
            FORMAT_PARAMS,
            "&action=wbgetentities&props=sitelinks",
            "&sites=", siteId, // defines the language of the titles
            "&sitefilter=", siteId, // defines for which languages sitelinks should be returned
            "&titles=", Utils.encodeUrl(String.join("|", titles))
        );
    }

}
