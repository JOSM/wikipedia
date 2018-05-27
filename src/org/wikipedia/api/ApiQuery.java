package org.wikipedia.api;

import java.net.URL;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;

public abstract class ApiQuery<T> {
    private final SerializationSchema<T> schema;
    private final URL url;

    protected ApiQuery(final URL url, final SerializationSchema<T> schema) {
        this.schema = schema;
        this.url = url;
    }

    public SerializationSchema<T> getSchema() {
        return schema;
    }

    public URL getUrl() {
        return url;
    }

    public String getCacheKey() {
        return url.toString();
    }
}
