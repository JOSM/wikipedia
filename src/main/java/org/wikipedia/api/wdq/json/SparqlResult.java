package org.wikipedia.api.wdq.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.wikipedia.api.SerializationSchema;

public class SparqlResult {
    public static final SerializationSchema<SparqlResult> SCHEMA = new SerializationSchema<>(
        SparqlResult.class,
        mapper -> mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    );

    private final Head head;
    private final Results results;

    @JsonCreator
    public SparqlResult(@JsonProperty("head") final Head head, @JsonProperty("results") final Results results) {
        this.head = Objects.requireNonNull(head);
        this.results = Objects.requireNonNull(results);
    }

    public Collection<String> getColumnLabels() {
        return Collections.unmodifiableCollection(head.vars);
    }

    public int getNumColumns() {
        return head.vars.size();
    }

    public int size() {
        return results.bindings.size();
    }

    public Results.Entry getEntry(final int rowIndex, final int columnIndex) {
        return results.bindings.get(rowIndex).get(head.vars.get(columnIndex));
    }

    public List<List<Results.Entry>> getRows() {
        return Collections.unmodifiableList(
            results.bindings.stream()
                .map(row -> head.vars.stream().map(row::get).collect(Collectors.toList()))
                .collect(Collectors.toList())
        );
    }

    private static class Head {
        private final List<String> vars;

        @JsonCreator
        public Head(@JsonProperty("vars") final List<String> vars) {
            this.vars = Objects.requireNonNull(vars);
        }
    }

    public static class Results {
        private final List<Map<String,Entry>> bindings;

        @JsonCreator
        public Results(@JsonProperty("bindings") final List<Map<String, Entry>> bindings) {
            this.bindings = Objects.requireNonNull(bindings);
        }

        public static class Entry {
            private final String type;
            private final String value;

            @JsonCreator
            public Entry(@JsonProperty("type") final String type, @JsonProperty("value") final String value) {
                this.type = Objects.requireNonNull(type);
                this.value = Objects.requireNonNull(value);
            }

            public String getType() {
                return type;
            }

            public String getValue() {
                return value;
            }
        }
    }
}
