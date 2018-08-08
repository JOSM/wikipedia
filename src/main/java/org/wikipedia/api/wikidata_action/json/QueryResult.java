package org.wikipedia.api.wikidata_action.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.wikipedia.api.SerializationSchema;

public final class QueryResult {
    public static final SerializationSchema<QueryResult> SCHEMA = new SerializationSchema<>(
        QueryResult.class,
        mapper -> mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    );

    private final Query query;

    @JsonCreator
    public QueryResult(@JsonProperty("query") final Query query) {
        this.query = query;
    }

    private static class Query {
        private final List<Language> languages;

        @JsonCreator
        Query(@JsonProperty("languages") final Collection<Language> languages) {
            this.languages = new ArrayList<>(languages);
        }

        private static class Language implements Comparable<Language> {
            private final String code;
            private final String name;

            @JsonCreator
            Language(@JsonProperty("code") final String code, @JsonProperty("*") final String name) {
                this.code = code;
                this.name = name;
            }

            @Override
            public int compareTo(Language other) {
                return this.code.compareTo(other.code);
            }
        }
    }

    public Map<String, String> getLangMap() {
        return query.languages.stream().collect(Collectors.toMap(it -> it.code, it -> it.name));
    }
}
