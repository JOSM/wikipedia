package org.wikipedia.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaPlugin;

public abstract class ApiQuery<T> {
    private final URL url;
    private final long cacheExpiryTime;
    private final IOExceptionFunction<InputStream, T> deserializeFunc;

    protected <S> ApiQuery(final URL url, final SerializationSchema<S> schema, final long cacheExpiryTime, final Function<S, T> resultConverter) {
        this.url = url;
        this.deserializeFunc = stream -> resultConverter.apply(schema.getMapper().readValue(stream, schema.getSchemaClass()));
        if (cacheExpiryTime >= 1) {
            this.cacheExpiryTime = cacheExpiryTime;
        } else {
            this.cacheExpiryTime = -1;
        }
    }

    protected ApiQuery(final URL url, final SerializationSchema<T> schema, final long cacheExpiryTime) {
        this(url, schema, cacheExpiryTime, it -> it);
    }

    protected ApiQuery(final URL url, final SerializationSchema<T> schema) {
        this(url, schema, -1);
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

    public final T deserialize(final InputStream stream) throws IOException {
        return deserializeFunc.apply(stream);
    }

    /**
     * @return the URL that is queried (might not contain the whole query, e.g. when a POST request is made)
     */
    public final URL getUrl() {
        return url;
    }

    protected final String getUserAgent(final String[] keywords) {
        final String result = String.format(
            "JOSM-wikipedia (%s). Report issues at %s.",
            WikipediaPlugin.getVersionInfo(),
            "https://josm.openstreetmap.de/newticket?component=Plugin%20wikipedia&priority=major&keywords=api"
        );
        if (keywords == null || keywords.length <= 0) {
            return result;
        }
        return result + Utils.encodeUrl(" " + String.join(" ", keywords));
    }

    @FunctionalInterface
    private interface IOExceptionFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
