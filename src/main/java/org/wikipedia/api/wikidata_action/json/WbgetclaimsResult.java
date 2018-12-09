package org.wikipedia.api.wikidata_action.json;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.api.CustomDeserializer;
import org.wikipedia.api.SerializationSchema;
import org.wikipedia.tools.RegexUtil;

public final class WbgetclaimsResult {

    public static final SerializationSchema<WbgetclaimsResult> SCHEMA = new SerializationSchema<>(
        WbgetclaimsResult.class,
        mapper -> {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.registerModule(new SimpleModule().addDeserializer(
                Collection.class,
                new Claim.Deserializer(mapper)
            ));
            mapper.registerModule(new SimpleModule().addDeserializer(
                Claim.MainSnak.DataValue.class,
                new Claim.MainSnak.DataValue.Deserializer(mapper)
            ));
        }
    );

    private final Collection<Claim> claims;

    @JsonCreator
    public WbgetclaimsResult(@JsonProperty(value = "claims", required = true) final Collection<Claim> claims) {
        this.claims = claims;
    }

    public Collection<Claim> getClaims() {
        return Collections.unmodifiableCollection(claims);
    }

    public static class Claim {
        public enum RANK { DEPRECATED, NORMAL, PREFERRED }

        private final String id;
        private final String type;
        private final RANK rank;
        private final MainSnak mainSnak;

        @JsonCreator
        public Claim(
            @JsonProperty("id") final String id,
            @JsonProperty("type") final String type,
            @JsonProperty("rank") final String rank,
            @JsonProperty("mainsnak") final MainSnak mainSnak
        ) {
            this.id = id;
            this.type = type;
            switch (rank) {
                case "deprecated": this.rank = RANK.DEPRECATED; break;
                case "preferred": this.rank = RANK.PREFERRED; break;
                default: this.rank = RANK.NORMAL;
                if (!"normal".equals(rank)) {
                    Logging.warn("Unknown rank of claim {0}!", id);
                }
            }
            this.mainSnak = mainSnak;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public RANK getRank() {
            return rank;
        }

        public MainSnak getMainSnak() {
            return mainSnak;
        }

        public static class MainSnak {
            final String property;
            final DataValue dataValue;
            private final String dataType;

            @JsonCreator
            MainSnak(
                @JsonProperty("property") final String property,
                @JsonProperty("datavalue") final DataValue dataValue,
                @JsonProperty("datatype") final String dataType
            ) {
                this.property = property;
                this.dataValue = dataValue;
                this.dataType = dataType;
            }

            public String getDataType() {
                return dataType;
            }

            public DataValue getDataValue() {
                return dataValue;
            }

            public String getProperty() {
                return property;
            }

            public interface DataValue {

                class Deserializer extends CustomDeserializer<DataValue> {
                    Deserializer(ObjectMapper mapper) {
                        super(mapper);
                    }

                    @Override
                    public DataValue deserialize(final JsonNode node) throws JsonProcessingException {
                        final JsonNode type = node.get("type");
                        final JsonNode nodeValue = node.get("value");
                        switch (type.textValue()) {
                            case "string": return new StringValue(mapper.treeToValue(nodeValue, String.class));
                            case "wikibase-entityid": return mapper.treeToValue(nodeValue, ItemValue.class);
                            case "quantity": return mapper.treeToValue(nodeValue, QuantityValue.class);
                            case "globecoordinate": return mapper.treeToValue(nodeValue, GlobecoordinateValue.class);
                            case "time": return mapper.treeToValue(nodeValue, TimeValue.class);
                            case "monolingualtext": return mapper.treeToValue(nodeValue, MonolingualTextValue.class);
                            default:
                                Logging.warn("Unknown type: " + type.textValue());
                        }
                        return null;
                    }
                }
            }

            static class GlobecoordinateValue implements DataValue {
                private final double latitude;
                private final double longitude;
                private final Double altitude;
                private final double precision;
                private final String globe;

                @JsonCreator
                GlobecoordinateValue(
                    @JsonProperty("latitude") final double latitude,
                    @JsonProperty("longitude") final double longitude,
                    @JsonProperty("altitude") final Double altitude,
                    @JsonProperty("precision") final double precision,
                    @JsonProperty("globe") final String globe
                ) {
                    this.latitude = latitude;
                    this.longitude = longitude;
                    this.altitude = altitude;
                    this.precision = precision;
                    this.globe = globe;
                }

                private LatLon toLatLon() {
                    return new LatLon(latitude, longitude);
                }

                @Override
                public String toString() {
                    return toLatLon().toDisplayString();
                }
            }

            static class TimeValue implements DataValue {
                private final String time;
                private final int timezone;
                private final int before;
                private final int after;
                private final int precision;
                private final String calendarModel;

                @JsonCreator
                TimeValue(
                    @JsonProperty("time") final String time,
                    @JsonProperty("timezone") final int timezone,
                    @JsonProperty("before") final int before,
                    @JsonProperty("after") final int after,
                    @JsonProperty("precision") final int precision,
                    @JsonProperty("calendarmodel") final String calendarModel

                ) {
                    this.time = time;
                    this.timezone = timezone;
                    this.before = before;
                    this.after = after;
                    this.precision = precision;
                    this.calendarModel = calendarModel;
                }

                @Override
                public String toString() {
                    return time;
                }
            }

            static class QuantityValue implements DataValue {
                private final double amount;
                private final String unit;
                private final double lowerBound;
                private final double upperBound;

                @JsonCreator
                QuantityValue(
                    @JsonProperty("amount") final double amount,
                    @JsonProperty("unit") final String unit,
                    @JsonProperty("lowerBound") final double lowerBound,
                    @JsonProperty("upperBound") final double upperBound
                ) {
                    this.amount = amount;
                    this.unit = unit;
                    this.lowerBound = lowerBound;
                    this.upperBound = upperBound;
                }

                @Override
                public String toString() {
                    return amount + " " + unit;
                }
            }

            static class ItemValue implements DataValue {
                private final String entityType;
                private final String id;

                @JsonCreator
                ItemValue(@JsonProperty("entity-type") final String entityType, @JsonProperty("id") final String id) {
                    this.entityType = entityType;
                    this.id = id;
                }

                @Override
                public String toString() {
                    return id + " (" + entityType + ")";
                }
            }

            static class StringValue implements DataValue {
                private final String value;
                StringValue(final String value) {
                    this.value = value;
                }

                @Override
                public String toString() {
                    return value;
                }
            }

            static class MonolingualTextValue implements DataValue {
                private final String langCode;
                private final String text;

                @JsonCreator
                MonolingualTextValue(@JsonProperty("language") final String langCode, @JsonProperty("text") final String text) {
                    this.langCode = langCode;
                    this.text = text;
                }

                @Override
                public String toString() {
                    return text + " (" + langCode + ")";
                }
            }
        }

        static class Deserializer extends StdDeserializer<Collection<Claim>> {
            private final ObjectMapper mapper;
            Deserializer(final ObjectMapper mapper) {
                super((Class<?>) null);
                this.mapper = mapper;
            }

            @Override
            public Collection<Claim> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                final Collection<Claim> result = new ArrayList<>();
                final JsonNode node = p.getCodec().readTree(p);
                final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fields.next();
                    if (RegexUtil.isValidPropertyId(field.getKey()) && field.getValue().isArray()) {
                        final Iterator<JsonNode> arrElements = field.getValue().elements();
                        while (arrElements.hasNext()) {
                            result.add(mapper.treeToValue(arrElements.next(), Claim.class));
                        }
                    }
                }
                return result;
            }
        }
    }
}
