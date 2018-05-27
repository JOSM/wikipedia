// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import java.util.function.Consumer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Wrapper class for the object mapper and the class to which you want to deserialize.
 * @param <T> The type which represents the JSON on the Java side.
 */
public class SerializationSchema<T> {
    public static final SerializationSchema<CheckEntityExistsResult> WBGETENTITIES = new SerializationSchema<>(
        CheckEntityExistsResult.class,
        it -> {
            it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            it.registerModule(new SimpleModule().addDeserializer(
                CheckEntityExistsResult.AbstractEntity.class,
                new CheckEntityExistsResult.AbstractEntity.Deserializer(it)
            ));
        }
    );

    private final ObjectMapper mapper;
    private final Class<T> schemaClass;

    private SerializationSchema(final Class<T> schemaClass, final Consumer<ObjectMapper> mapperConfig) {
        this.schemaClass = schemaClass;
        mapper = new ObjectMapper();
        mapperConfig.accept(mapper);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public Class<T> getSchemaClass() {
        return schemaClass;
    }
}
