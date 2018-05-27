// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import java.util.Objects;
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
    public static final SerializationSchema<SitematrixResult> SITEMATRIX = new SerializationSchema<>(
        SitematrixResult.class,
        mapper -> {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.registerModule(new SimpleModule().addDeserializer(
                SitematrixResult.Sitematrix.class,
                new SitematrixResult.Sitematrix.Deserializer(mapper)
            ));
        }
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> schemaClass;

    private SerializationSchema(final Class<T> schemaClass, final Consumer<ObjectMapper> mapperConfig) {
        Objects.requireNonNull(schemaClass);
        this.schemaClass = schemaClass;
        mapperConfig.accept(mapper); // configure the object mapper
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public Class<T> getSchemaClass() {
        return schemaClass;
    }
}
