package org.wikipedia.api.wdq;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiUrl;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.api.wdq.json.SparqlResult;
import org.wikipedia.tools.RegexUtil;
import org.wikipedia.tools.WikiProperties;

public class WdqApiQuery<T> extends ApiQuery<T> {
    private static final String[] TICKET_KEYWORDS = {"wikidata", "QueryService"};
    private static URL baseUrl = ApiUrl.url("https://query.wikidata.org/sparql");
    private final String queryString;

    private WdqApiQuery(final URL url, final String queryString, final SerializationSchema<T> schema) {
        super(url, schema);
        this.queryString = Objects.requireNonNull(queryString);
    }

    @Override
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

    static void setBaseUrl(final URL baseUrl) {
        Objects.requireNonNull(baseUrl);
        WdqApiQuery.baseUrl = baseUrl;
    }

    /**
     * @param items the items for which we check if they are instances of one of the items provided by {@code classes}
     *     or instances of any subclass of the items provided by {@code classes}.
     * @param classes the Q-IDs of items, for which the query checks if the provided items are instances of it,
     *     or instances of subclasses of it.
     * @return the API query
     */
    public static WdqApiQuery<SparqlResult> findInstancesOfClassesOrTheirSubclasses(final Collection<String> items, final Collection<String> classes) {
        Objects.requireNonNull(items);
        Objects.requireNonNull(classes);
        if (!items.isEmpty() && !classes.isEmpty() && Stream.concat(items.stream(), classes.stream()).allMatch(RegexUtil::isValidQId)) {
            return new WdqApiQuery<>(
                baseUrl,
                "format=json&query=" + Utils.encodeUrl(String.format(
                    "SELECT DISTINCT ?item ?itemLabel ?classes ?classesLabel WHERE { VALUES ?item { wd:%s }. VALUES ?classes { wd:%s }. ?item wdt:P31/wdt:P279* ?supertype. ?supertype wdt:P279* ?classes. SERVICE wikibase:label { bd:serviceParam wikibase:language \"%s\" }. }",
                    String.join(" wd:", items),
                    String.join(" wd:", classes),
                    WikiProperties.WIKIPEDIA_LANGUAGE.get()
                )),
                SparqlResult.SCHEMA
            );
        } else {
            throw new IllegalArgumentException("All arguments for the 'is instance of classes or their subclasses' check must be one or more valid Q-IDs!");
        }
    }
}
