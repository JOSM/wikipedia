// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikipedia_action;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.QueryString;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikipedia_action.json.QueryResult;
import org.wikipedia.data.IWikipediaSite;

public class WikipediaActionApiQuery<S, T> extends ApiQuery<T> {

    private static final QueryString FMT_PARAMS = new QueryString().plus("format", "json").plus("utf8", "1").plus("formatversion", "2");
    private static final String[] TICKET_KEYWORDS = {"wikipedia", "ActionAPI"};

    private final String queryString;

    private WikipediaActionApiQuery(
        final SitematrixResult.Sitematrix.Site site,
        final QueryString queryString,
        final SerializationSchema<S> schema,
        final long cacheExpiryTime,
        final Function<S, T> resultConverter
    ) {
        super(ApiUrl.url(site.getUrl() + "/w/api.php"), schema, cacheExpiryTime, resultConverter);
        this.queryString = Objects.requireNonNull(queryString).toString();
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
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setHeader("User-Agent", getUserAgent(TICKET_KEYWORDS))
            .setReasonForRequest(getQueryString().replace('&', ' '))
            .setRequestBody(getQueryString().getBytes(StandardCharsets.UTF_8));
    }

    public static WikipediaActionApiQuery<QueryResult, QueryResult> query(final SitematrixResult.Sitematrix.Site site, final Collection<String> titles) {
        Objects.requireNonNull(site);
        Objects.requireNonNull(titles);
        return new WikipediaActionApiQuery<>(
            site,
            FMT_PARAMS
                .plus("action", "query")
                .plus("redirects", 1)
                .plus("titles", String.join("|", titles)),
            QueryResult.SCHEMA,
            -1,
            it -> it
        );
    }

    public static WikipediaActionApiQuery<QueryResult, Optional<Set<QueryResult.Query.Page>>> categoryPrefixsearch(final SitematrixResult.Sitematrix.Site site, final String categoryPrefix) {
        return new WikipediaActionApiQuery<>(
            Objects.requireNonNull(site),
            FMT_PARAMS
                .plus("action", "query")
                .plus("list", "prefixsearch")
                .plus("psnamespace", QueryResult.Query.Page.CATEGORY_NAMESPACE)
                .plus("pslimit", 50)
                .plus("pssearch", categoryPrefix),
            QueryResult.SCHEMA,
            TimeUnit.DAYS.toMillis(3),
            it -> it.getQuery().getPrefixResults()
        );
    }
}
