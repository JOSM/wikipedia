package org.wikipedia.api;

import java.net.URL;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaPlugin;

public abstract class ApiQuery<T> {
    private final SerializationSchema<T> schema;
    private final URL url;
    private final long cacheExpiryTime;

    protected ApiQuery(final URL url, final SerializationSchema<T> schema, final long cacheExpiryTime) {
        this.schema = schema;
        this.url = url;
        if (cacheExpiryTime >= 1) {
            this.cacheExpiryTime = cacheExpiryTime;
        } else {
            this.cacheExpiryTime = -1;
        }
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

    public abstract String getApiName();

    public abstract HttpClient getHttpClient();

    /**
     * @return the schema that is used to get from the JSON encoded response to its Java representation
     */
    public final SerializationSchema<T> getSchema() {
        return schema;
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

}
