// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikipedia_action;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wikipedia_action.json.QueryResult;
import org.wikipedia.data.IWikipediaSite;

public class WikipediaActionApiQuery<T> extends ApiQuery<T> {

    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";
    private static final String[] TICKET_KEYWORDS = {"wikipedia", "ActionAPI"};

    private final String queryString;

    private WikipediaActionApiQuery(final IWikipediaSite site, final String queryString, SerializationSchema<T> schema) {
        super(ApiUrl.url(site.getSite().getUrl() + "/w/api.php"), schema);
        this.queryString = Objects.requireNonNull(queryString);
    }

    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getApiName() {
        return "Wikipedia Action API";
    }

    @Override
    public HttpClient getHttpClient() {
        return HttpClient.create(getUrl(), "POST")
            .setAccept("application/json")
            .setHeader("Content-Type", "text/plain; charset=utf-8")
            .setHeader("User-Agent", getUserAgent(TICKET_KEYWORDS))
            .setReasonForRequest(getQueryString().replace('&', ' '))
            .setRequestBody(getQueryString().getBytes(StandardCharsets.UTF_8));
    }

    public static WikipediaActionApiQuery<QueryResult> query(final IWikipediaSite site, final Collection<String> titles) {
        Objects.requireNonNull(site);
        Objects.requireNonNull(titles);
        return new WikipediaActionApiQuery<>(
            site,
            FORMAT_PARAMS +
                "&action=query&redirects=1&titles=" +
                Utils.encodeUrl(String.join("|", titles)),
            QueryResult.SCHEMA
        );
    }
}
