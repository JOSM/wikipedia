package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.QueryString;
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
    private static final QueryString FORMAT_PARAMS = new QueryString().plus(
        Pair.create("format", "json"),
        Pair.create("utf8", 1),
        Pair.create("formatversion", 2)
    );
    private static final String[] TICKET_KEYWORDS = {"wikidata", "MediawikiActionAPI"};

    private final QueryString queryString;

    /**
     * Convenience constructor
     * @param queryString the query string containing the arguments of this query
     * @param schema the {@link SerializationSchema} that defines how deserialization of the response is handled
     * @see #WikidataActionApiQuery(QueryString, SerializationSchema, long, Function)
     */
    private WikidataActionApiQuery(final QueryString queryString, final SerializationSchema<T> schema) {
        this(queryString, schema, -1, it -> it);
    }

    /**
     * A query against the Wikidata Action API
     * @param queryString the query string containing the arguments of this query
     * @param schema the {@link SerializationSchema} that defines how deserialization of the response is handled
     * @param cacheExpiryTime the number of milliseconds for which the response will stay in the cache
     * @param converter a function that maps the
     * @param <S> the type to which the {@link SerializationSchema} deserializes
     */
    private <S> WikidataActionApiQuery(final QueryString queryString, final SerializationSchema<S> schema, final long cacheExpiryTime, final Function<S, T> converter) {
        super(defaultUrl, schema, cacheExpiryTime, converter);
        this.queryString = queryString;
    }

    QueryString getQueryString() {
        return queryString;
    }

    /**
     * @return a query for all wikimedia sites
     */
    public static WikidataActionApiQuery<SitematrixResult.Sitematrix> sitematrix() {
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(Pair.create("action", "sitematrix")),
            SitematrixResult.SCHEMA,
            TimeUnit.DAYS.toMillis(90),
            SitematrixResult::getSitematrix
        );
    }

    /**
     * @return a query for all languages that are used in Wikidata (e.g. for labels)
     */
    public static WikidataActionApiQuery<Map<String, String>> queryLanguages() {
        return new WikidataActionApiQuery<>(
          FORMAT_PARAMS.plus(
              Pair.create("action", "query"),
              Pair.create("meta", "siteinfo"),
              Pair.create("siprop", "languages")
          ),
          QueryResult.SCHEMA,
          TimeUnit.DAYS.toMillis(90),
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
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("sites", ""),
                Pair.create("props", ""),
                Pair.create("ids", qIds)
            ),
            WbgetentitiesResult.SCHEMA
        );
    }

    public static WikidataActionApiQuery<WbgetentitiesResult> wbgetentities(final SitematrixResult.Sitematrix.Site site, final Collection<String> titles) {
        if (site == null || titles == null || titles.size() <= 0) {
            throw new IllegalArgumentException("The site and titles must be present!");
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("props", "sitelinks"),
                Pair.create("sites", site.getDbName()), // defines the language of the titles
                Pair.create("sitefilter", site.getDbName()), // defines for which languages sitelinks should be returned
                Pair.create("titles", titles)
            ),
            WbgetentitiesResult.SCHEMA
        );
    }


    public static WikidataActionApiQuery<Map<String, Optional<WbgetentitiesResult.Entity>>> wbgetentitiesLabels(final Collection<String> qId) {
        if (qId == null || !qId.stream().allMatch(RegexUtil::isValidQId)) {
            throw new IllegalArgumentException("Invalid Q-IDs: " + Optional.ofNullable(qId).map(it -> String.join(", ", it)).orElse("null"));
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("props", "aliases|descriptions|labels"),
                Pair.create("ids", qId)
            ),
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            result -> result.getSuccess() <= 0 ? null : qId.stream().collect(Collectors.toMap(it -> it, it -> result.getEntities().values().stream().filter(e -> it.equals(e.getId())).findFirst()))
        );
    }

    public static WikidataActionApiQuery<Optional<WbgetentitiesResult.Entity>> wbgetentitiesLabels(final String qId) {
        RegexUtil.requireValidQId(qId);
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("props", "aliases|descriptions|labels"),
                Pair.create("ids", qId)
            ),
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            result -> result.getEntities().values().stream().findFirst()
        );
    }

    public static WikidataActionApiQuery<Collection<WbgetentitiesResult.Entity.Sitelink>> wbgetentitiesSitelinks(final String qId) {
        RegexUtil.requireValidQId(qId);
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("props", "sitelinks"),
                Pair.create("ids", qId)
            ),
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            result -> result.getEntities().values().stream()
                .findFirst()
                .map(WbgetentitiesResult.Entity::getSitelinks)
                .orElse(Collections.emptyList())
        );
    }

    public static WikidataActionApiQuery<Optional<Collection<WbgetclaimsResult.Claim>>> wbgetentitiesClaims(final String qId) {
        RegexUtil.requireValidQId(qId);
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS.plus(
                Pair.create("action", "wbgetentities"),
                Pair.create("props", "claims"),
                Pair.create("ids", qId)
            ),
            WbgetentitiesResult.SCHEMA,
            TimeUnit.MINUTES.toMillis(10),
            it -> Optional.ofNullable(it.getEntities().get(qId)).flatMap(WbgetentitiesResult.Entity::getClaims)
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
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setHeader("User-Agent", getUserAgent(TICKET_KEYWORDS))
            .setReasonForRequest(getQueryString().toString().replace('&', ' '))
            .setRequestBody(getQueryString().toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String[] getTicketKeywords() {
        return TICKET_KEYWORDS;
    }
}
