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
import java.util.Map;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.api.SerializationSchema;

public final class QueryResult {
    public static final SerializationSchema<QueryResult> SCHEMA = new SerializationSchema<>(
        QueryResult.class,
        mapper -> {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.registerModule(new SimpleModule().addDeserializer(
               QueryResult.Query.Redirects.class,
               new Query.Redirects.Deserializer()
            ));
            mapper.registerModule(new SimpleModule().addDeserializer(
               QueryResult.Query.Pages.class,
               new Query.Pages.Deserializer(mapper)
            ));
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

        private final Redirects redirects;
        private final Pages pages;

        @JsonCreator
        public Query(@JsonProperty("redirects") final Redirects redirects, @JsonProperty("pages") final Pages pages) {
            this.redirects = redirects == null ? new Redirects() : redirects;
            this.pages = pages == null ? new Pages() : pages;
        }

        public Collection<Pages.Page> getPages() {
            return Collections.unmodifiableCollection(pages.pages);
        }

        public Redirects getRedirects() {
            return redirects;
        }

        public static final class Redirects {
            private final Map<String, String> redirectMap = new HashMap<>();

            private Redirects() { }

            public String resolveRedirect(final String from) {
                if (!redirectMap.containsKey(from)) {
                    return from;
                } else if (redirectMap.containsKey(from) && !redirectMap.containsKey(redirectMap.get(from))) {
                    return redirectMap.get(from);
                } else {
                    final Deque<String> redirectChain = new ArrayDeque<>();
                    redirectChain.add(from);
                    while (redirectMap.containsKey(redirectChain.getLast())) {
                        final String newVal = redirectMap.get(redirectChain.getLast());
                        if (redirectChain.contains(newVal)) {
                            Logging.warn("Circular redirect in Wikipedia detected: " + String.join(" → ", redirectChain));
                            return from;
                        } else {
                            redirectChain.add(newVal);
                        }
                    }
                    // Shortcut for future requests: Redirect all elements in the redirect chain to the last element.
                    while (redirectChain.size() >= 2) {
                        redirectMap.put(redirectChain.pollFirst(), redirectChain.getLast());
                    }
                    return redirectChain.getLast();
                }

            }

            static class Deserializer extends StdDeserializer<Redirects> {
                Deserializer() {
                    super((Class<?>) null);
                }

                @Override
                public Redirects deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                    final JsonNode node = p.getCodec().readTree(p);
                    final Redirects result = new Redirects();
                    node.elements().forEachRemaining(e -> {
                        final JsonNode from = e.get("from");
                        final JsonNode to = e.get("to");
                        if (from.isTextual() && to.isTextual()) {
                            result.redirectMap.put(from.textValue(), to.textValue());
                        }
                    });
                    return result;
                }
            }
        }

        public static class Pages {
            private final Collection<Page> pages = new ArrayList<>();

            public static class Page {
                private final String title;

                @JsonCreator
                public Page(@JsonProperty("title") final String title) {
                    this.title = title;
                }

                public String getTitle() {
                    return title;
                }
            }

            static class Deserializer extends StdDeserializer<Pages> {

                private final ObjectMapper mapper;

                Deserializer(final ObjectMapper mapper) {
                    super((Class<?>) null);
                    this.mapper = mapper;
                }

                @Override
                public Pages deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
                    final JsonNode node = p.getCodec().readTree(p);
                    final Collection<JsonProcessingException> exception = new ArrayList<>();
                    final Pages pages = new Pages();
                    node.fields().forEachRemaining(entry -> {
                        try {
                            pages.pages.add(mapper.treeToValue(entry.getValue(), Page.class));
                        } catch (JsonProcessingException e) {
                            exception.add(e);
                        }
                    });
                    if (exception.size() >= 1) {
                        throw exception.iterator().next();
                    }
                    return pages;
                }
            }
        }
    }
}
