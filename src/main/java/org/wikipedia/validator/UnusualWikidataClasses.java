package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wdq.WdqApiQuery;
import org.wikipedia.api.wdq.json.SparqlResult;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.RegexUtil;
import org.wikipedia.tools.WikiProperties;

public class UnusualWikidataClasses extends BatchProcessedTagTest<UnusualWikidataClasses.TestCompanion> {
    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not check for unusual classes in wikidata=* tags.") +
        "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    public UnusualWikidataClasses() {
        super(
            I18n.tr("Find OSM objects linked with wikidata items of a class that is untypical for OSM"),
            I18n.tr("This check queries Wikidata to find those OSM objects that are linked to wikidata items of a type, which should not occur in OSM data (at least not as the main wikidata tag)")
        );
    }

    @Override
    protected TestCompanion prepareTestCompanion(OsmPrimitive primitive) {
        final String wikidataValue = primitive.get("wikidata");
        if (RegexUtil.isValidQId(wikidataValue)) {
            return new TestCompanion(primitive, wikidataValue);
        }
        return null;
    }

    @Override
    protected void check(List<TestCompanion> allPrimitives) {
        ListUtil.processInBatches(allPrimitives, 50, batch -> {
            for (final String forbiddenType : WikiProperties.WIKIDATA_VALIDATOR_UNUSUAL_CLASSES.get()) {
                try {
                    checkBatch(batch, forbiddenType);
                } catch (IOException e) {
                    errors.add(
                        AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                            .primitives(batch.stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive).collect(Collectors.toList()))
                            .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + e.getMessage())
                            .build()
                    );
                }
            }
        });
    }

    private void checkBatch(final Collection<TestCompanion> batch, final String forbiddenType) throws IOException {
        final SparqlResult result = ApiQueryClient.query(WdqApiQuery.findInstancesOfXOrOfSubclass(batch.stream().map(it -> it.wikidataValue).collect(Collectors.toList()), forbiddenType));
        for (List<SparqlResult.Results.Entry> row : result.getRows()) {
            final String entityURL = row.get(0).getValue();
            final String qID = entityURL.substring(entityURL.lastIndexOf('/') >= 0 ? entityURL.lastIndexOf('/') + 1 : 0);
            final Collection<OsmPrimitive> primitives = batch.stream()
                .filter(it -> qID.equals(it.wikidataValue))
                .map(BatchProcessedTagTest.TestCompanion::getPrimitive)
                .collect(Collectors.toList());
            if (primitives.size() >= 1) {
                errors.add(
                    AllValidationTests.WIKIDATA_TAG_HAS_UNUSUAL_TYPE.getBuilder(this)
                        .primitives(primitives)
                        .message(
                            "Wikidata value is of unusual type for the wikidata=* tag on OSM objects",
                            I18n.marktr("{0} is an instance of {1} (or any subclass thereof)"),
                            qID,
                            forbiddenType
                        )
                        .build()
                );
            }
        }
    }

    static class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        private final String wikidataValue;

        TestCompanion(final OsmPrimitive primitive, final String wikidataValue) {
            super(primitive);
            this.wikidataValue = wikidataValue;
        }
    }
}
