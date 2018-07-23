package org.wikipedia.api.wikidata_action;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikidata_action.json.WbgetclaimsResult;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.tools.RegexUtil;

public final class WikidataActionApiQuery<T> extends ApiQuery<T> {
    static URL defaultUrl = ApiUrl.url("https://www.wikidata.org/w/api.php");
    private static final String FORMAT_PARAMS = "format=json&utf8=1&formatversion=1";
    private static final String[] TICKET_KEYWORDS = {"wikidata", "ActionAPI"};

    private final String queryString;

    private WikidataActionApiQuery(final String query, final SerializationSchema<T> schema, final long cacheExpiryTime) {
        super(defaultUrl, schema, cacheExpiryTime);
        this.queryString = query;
    }

    private WikidataActionApiQuery(final String query, final SerializationSchema<T> schema) {
        this(query, schema, -1);
    }

    private <S> WikidataActionApiQuery(final String queryString, final SerializationSchema<S> schema, final Function<S, T> converter) {
        super(defaultUrl, schema, -1, converter);
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

    public static WikidataActionApiQuery<SitematrixResult> sitematrix() {
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=sitematrix",
            SitematrixResult.SCHEMA,
            2_592_000_000L // = 1000*60*60*24*30 = number of ms in 30 days
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

    public static WikidataActionApiQuery<Map<String, String>> wbgetentitiesLabels(final String qId) {
        if (!RegexUtil.isValidQId(qId)) {
            throw new IllegalArgumentException("Invalid Q-ID: " + qId);
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetentities&props=labels&ids=" + qId,
            WbgetentitiesResult.SCHEMA,
            result -> result.getEntities().values().stream().findFirst().map(WbgetentitiesResult.Entity::getLabels).orElse(new HashMap<>())
        );
    }

    public static WikidataActionApiQuery<WbgetclaimsResult> wbgetclaims(final String qId) {
        if (!RegexUtil.isValidQId(qId)) {
            throw new IllegalArgumentException("Invalid Q-ID: " + qId);
        }
        return new WikidataActionApiQuery<>(
            FORMAT_PARAMS + "&action=wbgetclaims&props=&entity=" + qId,
            WbgetclaimsResult.SCHEMA
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
