// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CheckEntityExistsResult {
    private final int success;
    private final Map<String, Entity> entities;

    @JsonCreator
    public CheckEntityExistsResult(@JsonProperty("success") final int success, @JsonProperty("entities") final Map<String, Entity> entities) {
        this.success = success;
        this.entities = entities;
    }

    public int getSuccess() {
        return success;
    }

    public Map<String, Entity> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    public static final class Entity {
        private final String id;
        private final String type;

        @JsonCreator
        public Entity(@JsonProperty("id") final String id, @JsonProperty("type") final String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }
}