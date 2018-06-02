package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.util.Collection;

import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.wikidata_action.json.CheckEntityExistsResult;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.tools.RegexUtil;

public class WikidataActionApiQuery<T> extends ApiQuery<T> {
    static URL defaultUrl = ApiUrl.url("https://www.wikidata.org/w/api.php");
    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";

    private final String query;

    private WikidataActionApiQuery(final String query, final SerializationSchema<T> schema) {
        super(defaultUrl, schema);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public static WikidataActionApiQuery<SitematrixResult> sitematrix() {
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=sitematrix",
            SerializationSchema.SITEMATRIX
        );
    }

    public static WikidataActionApiQuery<CheckEntityExistsResult> wbgetentities(final String siteId, final Collection<String> titles) {
        if (siteId == null || titles == null || titles.size() <= 0) {
            throw new IllegalArgumentException("The site ID and titles must be present!");
        }
        if (!RegexUtil.isValidSiteId(siteId)) {
            throw new IllegalArgumentException("The site ID is not given in the expected format!");
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetentities&props=sitelinks&sites=" + siteId + // defines the language of the titles
            "&sitefilter=" + siteId + // defines for which languages sitelinks should be returned
            "&titles=" + Utils.encodeUrl(String.join("|", titles)),
            SerializationSchema.WBGETENTITIES
        );
    }

    @Override
    public String getCacheKey() {
        return getUrl().toString() + '?' + getQuery();
    }
}
