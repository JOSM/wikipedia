package org.wikipedia.api.wdq;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wdq.json.SparqlResult;
import org.wikipedia.tools.RegexUtil;

public class WdqApiQuery<T> extends ApiQuery<T> {
    private static final String[] TICKET_KEYWORDS = {"wikidata", "QueryService"};
    private final String queryString;

    public WdqApiQuery(final URL url, final String queryString, final SerializationSchema<T> schema) {
        super(url, schema, -1);
        this.queryString = Objects.requireNonNull(queryString);
    }

    public String getApiName() {
        return "Wikidata Query Service API";
    }

    @Override
    public HttpClient getHttpClient() {
        return HttpClient.create(getUrl(), "POST")
            .setAccept("application/sparql-results+json")
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setHeader("User-Agent", getUserAgent(TICKET_KEYWORDS))
            .setReasonForRequest(queryString.replace('&', ' '))
            .setRequestBody(queryString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param items the items for which we check if they are instances of {@code x}
     *     or instances of any subclass of {@code x}.
     * @param x the Q-ID of an item, for which the query checks if the provided items are instances of it,
     *     or instances of subclasses of it.
     * @return the API query
     */
    public static WdqApiQuery<SparqlResult> findInstancesOfXOrOfSubclass(final Collection<String> items, final String x) {
        Objects.requireNonNull(items);
        Objects.requireNonNull(x);
        if (items.size() <= 0 || !items.stream().allMatch(RegexUtil::isValidQId) || !RegexUtil.isValidQId(x)) {
            throw new IllegalArgumentException("All arguments for the 'is instance of X or of subclass' check must be valid Q-IDs!");
        }
        return new WdqApiQuery<>(
            ApiUrl.url("https://query.wikidata.org/sparql"),
            "format=json&query=" + Utils.encodeUrl(String.format("SELECT DISTINCT ?item WHERE { VALUES ?item { wd:%s } ?item wdt:P31/wdt:P279* wd:%s. }", String.join(" wd:", items), x)),
            SparqlResult.SCHEMA
        );
    }
}
