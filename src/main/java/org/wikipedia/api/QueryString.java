package org.wikipedia.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

// @javax.annotation.concurrent.Immutable
public class QueryString {
    private final Map<String, String> parameters;
    /**
     * This separator ensures that values could contain the pipe character {@code |} if needed
     * (see <a href="https://www.mediawiki.org/wiki/API:Data_formats#Multivalue_parameters">Mediawiki API documentation on multivalue parameters</a>)
     */
    private static final CharSequence MULTI_VALUE_SEPARATOR = "\u001F";

    public QueryString() {
        this(new HashMap<>());
    }

    private QueryString(final Map<String, String> parameters) {
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * Add new parameters to the query string.
     *
     * You can pass any object as key or as value, it will be converted to a string using {@link Object#toString()}.
     *
     * If a {@link Collection} is passed as value, each element of the collection is converted to a string individually
     * using {@link Object#toString()}. Then they are concatenated with {@link #MULTI_VALUE_SEPARATOR} as separator and
     * also prepended by one {@link #MULTI_VALUE_SEPARATOR}.
     *
     * @param params an array of new parameters, given as pair of key and value.
     * @return a new QueryString containing the parameters of this query string plus the new ones given in the parameter.
     *   This method does not modify the existing object on which the method is called.
     */
    @SafeVarargs
    public final QueryString plus(final Pair<Object, Object>... params) {
        final Map<String, String> result = new HashMap<>(parameters);
        Arrays.stream(Objects.requireNonNull(params)).forEach(it -> {
            final Object key = Objects.requireNonNull(it.a);
            final Object value;
            if (it.b instanceof Collection) {
                value = ((Collection<?>) it.b).stream()
                    .map(Object::toString)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(/* delimiter= */ MULTI_VALUE_SEPARATOR, /* prefix= */ ((Collection) it.b).size() > 1 ? MULTI_VALUE_SEPARATOR : "", /* suffix= */ ""));
            } else {
                value = it.b.toString();
            }
            result.put(key.toString(), value.toString());
        });
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
