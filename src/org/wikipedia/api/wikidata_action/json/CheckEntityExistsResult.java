// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public final class CheckEntityExistsResult {

    private final int success;
    private final Map<String, Entity> entities = new HashMap<>();
    private final List<MissingEntity> missingEntities = new ArrayList<>();

    @JsonCreator
    public CheckEntityExistsResult(@JsonProperty("success") final int success, @JsonProperty("entities") final Map<String, AbstractEntity> entities) {
        this.success = success;
        for (final Map.Entry<String, AbstractEntity> entry : entities.entrySet()) {
            entry.getValue().addTo(entry.getKey(), this);
        }
    }

    /**
     * @return the success-value of the result, 1 means success, other values mean failure
     */
    public int getSuccess() {
        return success;
    }

    /**
     * @return All entities that were found, the values are the entities itself, the key is the Q-ID of the entity.
     *     (but better rely on the Q-ID provided by the {@link Entity} object, I'm not sure but sometimes the key
     *     might be a redirect to the Q-ID that the entity provides??!)
     */
    public Map<String, Entity> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    /**
     * @return all the entities that are reported as missing
     */
    public Collection<MissingEntity> getMissingEntities() {
        return Collections.unmodifiableCollection(missingEntities);
    }

    /**
     * Supertype for {@link MissingEntity} and {@link Entity}
     */
    interface AbstractEntity {
        /**
         * Adds this entity to the entity lists/maps of {@link CheckEntityExistsResult}, depending on the implementing
         * class it can vary to which list or map the entity is added.
         * @param key the key to which the entity is associated in the JSON
         * @param result the object to which the entity should be added
         */
        void addTo(final String key, CheckEntityExistsResult result);

        class Deserializer extends StdDeserializer<AbstractEntity> {
            private final ObjectMapper mapper;
            Deserializer(final ObjectMapper mapper) {
                super((Class<?>) null);
                this.mapper = mapper;
            }

            @Override
            public AbstractEntity deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException, JsonProcessingException
            {
                final JsonNode node = p.getCodec().readTree(p);
                if (node.has("missing")) {
                    return mapper.treeToValue(node, MissingEntity.class);
                }
                return mapper.treeToValue(node, Entity.class);
            }
        }
    }

    public static final class MissingEntity implements AbstractEntity {
        private final String id;
        private final String site;
        private final String title;
        @JsonCreator
        public MissingEntity(
            @JsonProperty("id") final String id,
            @JsonProperty("site") final String site,
            @JsonProperty("title") final String title
        ) {
            this.id = id;
            this.site = site;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public String getSite() {
            return site;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public void addTo(final String key, final CheckEntityExistsResult result) {
            result.missingEntities.add(this);
        }
    }

    public static final class Entity implements AbstractEntity {
        private final String id;
        private final String type;
        private final Map<String, Sitelink> sitelinks = new HashMap<>();

        @JsonCreator
        public Entity(
            @JsonProperty("id") final String id,
            @JsonProperty("type") final String type,
            @JsonProperty("sitelinks") final Map<String, Sitelink> sitelinks
        ) {
            this.id = id;
            this.type = type;
            if (sitelinks != null) {
                this.sitelinks.putAll(sitelinks);
            }
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public Collection<Sitelink> getSitelinks() {
            return Collections.unmodifiableCollection(sitelinks.values());
        }

        @Override
        public void addTo(final String key, final CheckEntityExistsResult result) {
            result.entities.put(key, this);
        }

        public static final class Sitelink {
            private final String site;
            private final String title;

            @JsonCreator
            public Sitelink(@JsonProperty("site") final String site, @JsonProperty("title") final String title) {
                this.site = site;
                this.title = title;
            }

            public String getSite() {
                return site;
            }

            public String getTitle() {
                return title;
            }
        }
    }
}
