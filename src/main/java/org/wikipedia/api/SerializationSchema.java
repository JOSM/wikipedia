// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Wrapper class for the object mapper and the class to which you want to deserialize.
 * @param <T> The type which represents the JSON on the Java side.
 */
public class SerializationSchema<T> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> schemaClass;

    public SerializationSchema(final Class<T> schemaClass, final Consumer<ObjectMapper> mapperConfig) {
        Objects.requireNonNull(schemaClass);
        this.schemaClass = schemaClass;
        mapperConfig.accept(mapper); // configure the object mapper
    }

    public final ObjectMapper getMapper() {
        return mapper;
    }

    public final Class<T> getSchemaClass() {
        return schemaClass;
    }
}
