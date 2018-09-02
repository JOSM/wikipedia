package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wikidata_action.json.QueryResult;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikidata_action.json.WbgetclaimsResult;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.tools.RegexUtil;

/**
 * Utility class for getting queries against the Wikidata Action API
 * @param <T> the type which is returned as a result of the query
 */
public final class WikidataActionApiQuery<T> extends ApiQuery<T> {
    static URL defaultUrl = ApiUrl.url("https://www.wikidata.org/w/api.php");
    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";
    private static final String[] TICKET_KEYWORDS = {"wikidata", "ActionAPI"};

    private final String queryString;

    /**
     * Convenience constructor
     * @param queryString the query string containing the arguments of this query
     * @param schema the {@link SerializationSchema} that defines how deserialization of the response is handled
     * @see #WikidataActionApiQuery(String, SerializationSchema, long, Function)
     */
    private WikidataActionApiQuery(final String queryString, final SerializationSchema<T> schema) {
        this(queryString, schema, -1, it -> it);
    }

    /**
     * Convenience constructor
     * @param queryString the query string containing the arguments of this query
     * @param schema the {@link SerializationSchema} that defines how deserialization of the response is handled
     * @param converter a function that maps the
     * @see #WikidataActionApiQuery(String, SerializationSchema, long, Function)
     */
    private <S> WikidataActionApiQuery(final String queryString, final SerializationSchema<S> schema, final Function<S, T> converter) {
        this(queryString, schema, -1, converter);
    }

    /**
     * A query against the Wikidata Action API
     * @param queryString the query string containing the arguments of this query
     * @param schema the {@link SerializationSchema} that defines how deserialization of the response is handled
     * @param cacheExpiryTime the number of milliseconds for which the response will stay in the cache
     * @param converter a function that maps the
     * @param <S> the type to which the {@link SerializationSchema} deserializes
     */
    private <S> WikidataActionApiQuery(final String queryString, final SerializationSchema<S> schema, final long cacheExpiryTime, final Function<S, T> converter) {
        super(defaultUrl, schema, cacheExpiryTime, converter);
        this.queryString = queryString;
    }

    String getQueryString() {
        return queryString;
    }

    /**
     * @return a query for all wikimedia sites
     */
    public static WikidataActionApiQuery<SitematrixResult.Sitematrix> sitematrix() {
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=sitematrix",
            SitematrixResult.SCHEMA,
            TimeUnit.DAYS.toMillis(30),
            SitematrixResult::getSitematrix
        );
    }

    /**
     * @return a query for all languages that are used in Wikidata (e.g. for labels)
     */
    public static WikidataActionApiQuery<Map<String, String>> queryLanguages() {
        return new WikidataActionApiQuery<>(
          FORMAT_PARAMS + "&action=query&meta=siteinfo&siprop=languages",
          QueryResult.SCHEMA,
          TimeUnit.DAYS.toMillis(30),
          QueryResult::getLangMap
        );
    }

    public static WikidataActionApiQuery<WbgetentitiesResult> wbgetentities(final Collection<String> qIds) {
        if (qIds.size() < 1) {
            throw new IllegalArgumentException("You must supply at least one Q-ID to construct a checkEntityExists URL.");
        }
        if (!qIds.stream().allMatch(RegexUtil::isValidQId)) {
            throw new IllegalArgumentException("You must supply only Q-IDs as argument to construct a checkEntityExists URL.");
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetentities&sites=&props=&ids=" + Utils.encodeUrl(String.join("|", qIds)),
            WbgetentitiesResult.SCHEMA
        );
    }

    public static WikidataActionApiQuery<WbgetentitiesResult> wbgetentities(final String siteId, final Collection<String> titles) {
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
            WbgetentitiesResult.SCHEMA
        );
    }

    public static WikidataActionApiQuery<Optional<WbgetentitiesResult.Entity>> wbgetentitiesLabels(final String qId) {
        if (!RegexUtil.isValidQId(qId)) {
            throw new IllegalArgumentException("Invalid Q-ID: " + qId);
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetentities&props=labels|descriptions|aliases&ids=" + qId,
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            result -> result.getEntities().values().stream().findFirst()
        );
    }

    public static WikidataActionApiQuery<Collection<WbgetentitiesResult.Entity.Sitelink>> wbgetentitiesSitelinks(final String qId) {
        RegexUtil.requireValidQId(qId);
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetentities&props=sitelinks&ids=" + qId,
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            result -> result.getEntities().values().stream()
                .findFirst()
                .map(WbgetentitiesResult.Entity::getSitelinks)
                .orElse(Collections.emptyList())
        );
    }

    public static WikidataActionApiQuery<Collection<WbgetclaimsResult.Claim>> wbgetclaims(final String qId) {
        if (!RegexUtil.isValidQId(qId)) {
            throw new IllegalArgumentException("Invalid Q-ID: " + qId);
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetclaims&props=&entity=" + qId,
            WbgetclaimsResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            WbgetclaimsResult::getClaims
        );
    }

    @Override
    public String getCacheKey() {
        return getUrl().toString() + '?' + getQueryString();
    }

    @Override
    public String getApiName() {
        return "Wikidata Action API";
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
}
