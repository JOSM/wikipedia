package org.wikipedia.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public class QueryString {
    private final Map<String, String> parameters;

    public QueryString() {
        this(new HashMap<>());
    }

    private QueryString(final Map<String, String> parameters) {
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    @SafeVarargs
    public final QueryString plus(final Pair<Object, Object>... params) {
        final Map<String, String> result = new HashMap<>(parameters);
        Arrays.stream(Objects.requireNonNull(params)).forEach(it -> result.put(Objects.requireNonNull(it.a).toString(), Objects.requireNonNull(it.b).toString()));
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
