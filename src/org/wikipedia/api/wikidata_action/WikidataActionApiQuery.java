package org.wikipedia.api.wikidata_action;

import java.net.URL;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;

public class WikidataActionApiQuery<T> extends ApiQuery<T> {
    private static final URL URL = ApiUrl.url("https://www.wikidata.org/w/api.php");
    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";

    private final String query;

    private WikidataActionApiQuery(final String query, final SerializationSchema<T> schema) {
        super(URL, schema);
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

    @Override
    public String getCacheKey() {
        return getUrl().toString() + '?' + getQuery();
    }
}
