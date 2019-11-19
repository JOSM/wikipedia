package org.wikipedia.api;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openstreetmap.josm.tools.Utils;

public class QueryString {
    private final Map<String, String> parameters;

    public QueryString() {
        this(new HashMap<>());
    }

    private QueryString(final Map<String, String> parameters) {
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public QueryString plus(final Object key, final Object value) {
        final Map<String, String> result = new HashMap<>(parameters);
        result.put(Objects.requireNonNull(key).toString(), Objects.requireNonNull(value).toString());
        return new QueryString(result);
    }

    @Override
    public String toString() {
        return parameters.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(it -> Utils.encodeUrl(it.getKey()) + "=" + Utils.encodeUrl(it.getValue()))
            .collect(Collectors.joining("&"));
    }
}
