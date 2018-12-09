package org.wikipedia.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Objects;

public abstract class CustomDeserializer<T> extends StdDeserializer<T> {
    protected final ObjectMapper mapper;

    protected CustomDeserializer(final ObjectMapper mapper) {
        super((Class<?>) null);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public final T deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return deserialize(p.getCodec().readTree(p));
    }

    public abstract T deserialize(final JsonNode node) throws JsonProcessingException;
}
