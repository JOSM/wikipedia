package org.wikipedia.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.WikipediaPlugin;

public abstract class ApiQuery<T> {
    private final URL url;
    private final long cacheExpiryTime;
    protected final IOExceptionFunction<InputStream, T> deserializeFunc;

    protected <S> ApiQuery(final URL url, final SerializationSchema<S> schema, final long cacheExpiryTime, final Function<S, T> resultConverter) {
        this.url = url;
        this.deserializeFunc = stream -> resultConverter.apply(schema.getMapper().readValue(stream, schema.getSchemaClass()));
        if (cacheExpiryTime >= 1) {
            this.cacheExpiryTime = cacheExpiryTime;
        } else {
            this.cacheExpiryTime = -1;
        }
    }

    protected ApiQuery(final URL url, final SerializationSchema<T> schema) {
        this(url, schema, -1, it -> it);
    }

    /**
     * @return the number of milliseconds for which the API response should be retrieved
     *     from the cache without trying to get it directly from the API
     */
    final long getCacheExpiryTime() {
        return cacheExpiryTime;
    }

    /**
     * A string containing the API endpoint and the query sent to that endpoint.
     * Normally this is a URL, but don't rely on that.
     * @return the key for which the response will be cached
     */
    public String getCacheKey() {
        return url.toString();
    }

    /**
     * @return a (preferably short) name that identifies the API for which the query is designed.
     */
    public abstract String getApiName();

    /**
     * @return for each call a new instance of a {@link HttpClient} ready to execute the query
     */
    public abstract HttpClient getHttpClient();

    public abstract String[] getTicketKeywords();

    /**
     * @return the URL that is queried (might not contain the whole query, e.g. when a POST request is made)
     */
    public final URL getUrl() {
        return url;
    }

    private final QueryString tracQueryString = new QueryString().plus(Pair.create("component", "Plugin wikipedia"), Pair.create("priority", "major"));
    protected final String getUserAgent(final String[] keywords) {
        final Stream<String> keywordStream = Optional.ofNullable(keywords).filter(it -> it.length > 0).map(it -> Stream.of(keywords)).orElse(Stream.empty());
        final String url = "https://josm.openstreetmap.de/newticket?" + tracQueryString.plus(Pair.create("keywords", Stream.concat(keywordStream, Stream.of("api")).collect(Collectors.joining(" "))));
        return String.format(
            "JOSM-wikipedia (%s). Report issues at %s .",
            WikipediaPlugin.getVersionInfo(),
            url
        );
    }

    @FunctionalInterface
    protected interface IOExceptionFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
