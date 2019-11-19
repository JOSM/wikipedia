// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikipedia_action.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.api.SerializationSchema;

public final class QueryResult {
    public static final SerializationSchema<QueryResult> SCHEMA = new SerializationSchema<>(
        QueryResult.class,
        mapper -> {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
    );

    private final Query query;

    @JsonCreator
    public QueryResult(@JsonProperty("query") final Query query) {
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }

    public static final class Query {

        private final Set<Redirect> normalized;
        private final Set<Redirect> redirects;
        private final List<Page> pages;
        private final Optional<Set<Page>> prefixResults;

        @JsonCreator
        public Query(
            @JsonProperty("normalized") final Set<Redirect> normalized,
            @JsonProperty("redirects") final Set<Redirect> redirects,
            @JsonProperty("pages") final List<Page> pages,
            @JsonProperty("prefixsearch") final Set<Page> prefixResults
        ) {
            this.normalized = Optional.ofNullable(normalized).orElse(new HashSet<>());
            this.redirects = Optional.ofNullable(redirects).orElse(new HashSet<>());
            this.pages = pages == null ? Collections.unmodifiableList(new ArrayList<>()) : pages;
            this.prefixResults = Optional.ofNullable(prefixResults).map(Collections::unmodifiableSet);
        }

        public Collection<Page> getPages() {
            return Collections.unmodifiableCollection(pages);
        }

        public Optional<Set<Page>> getPrefixResults() {
            return prefixResults;
        }

        public Set<Redirect> getRedirects() {
            return redirects;
        }

        public String resolveRedirect(final String from) {
            return resolveRedirect(from, from, 0);
        }

        private String resolveRedirect(final String from, final String current, int depth) {
            if (depth > 10) {
                Logging.warn("Wikipedia page '{0}' redirects more than 10 times, probably a circular redirect. I'm not going to resolve this redirect.", from);
                return from;
            }
            if (depth > 0 && from.equals(current)) {
                Logging.warn("Wikipedia page " + from + " is part of a circular redirect of size " + depth + " (redirects back onto itself)!");
            }
            final Optional<Redirect> redirect = Stream.concat(normalized.stream(), redirects.stream()).filter(it -> current.equals(it.from)).findFirst();
            if (redirect.isPresent()) {
                return resolveRedirect(from, redirect.get().getTo(), depth + 1);
            }
            return current;
        }

        public static final class Redirect {
            private final String from;
            private final String to;

            @JsonCreator
            public Redirect(
                @JsonProperty("from") final String from,
                @JsonProperty("to") final String to
            ) {
                this.from = from;
                this.to = to;
            }

            public String getTo() {
                return to;
            }
        }

        public static class Page {
            public static final int CATEGORY_NAMESPACE = 14;

            private final String title;
            private final int pageId;
            private final int namespace;

            @JsonCreator
            public Page(
                @JsonProperty("ns") final int namespace,
                @JsonProperty("title") final String title,
                @JsonProperty("pageid") final int pageId
            ) {
                this.title = title;
                this.namespace = namespace;
                this.pageId = pageId;
            }

            public int getNamespace() {
                return namespace;
            }

            public int getPageId() {
                return pageId;
            }

            public String getTitle() {
                return title;
            }
        }
    }
}
