// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.RegexUtil;

/**
 * Checks if for the wikidata=* tag on an {@link OsmPrimitive} a Wikidata item really exists.
 * This check requires a working internet connection, because it queries the Wikidata Action API.
 */
public class WikidataItemExists extends BatchProcessedTagTest<WikidataItemExists.TestCompanion> {

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not validate all wikidata=* tags over the internet.") + "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    private static final int BATCH_SIZE = 50;


    public WikidataItemExists() {
        super(
            I18n.tr("wikidata=* item exists"),
            I18n.tr("Make sure the Wikidata item for the Q-ID in the wikidata=* tag actually exists")
        );
    }

    @Override
    protected void check(List<TestCompanion> allPrimitives) {
        ListUtil.processInBatches(allPrimitives, BATCH_SIZE, primitiveBatch -> {
            try {
                final WbgetentitiesResult entityQueryResult = ApiQueryClient.query(
                    WikidataActionApiQuery.wbgetentities(primitiveBatch.stream().map(tc -> tc.wikidataId).collect(Collectors.toList()))
                );
                if (entityQueryResult.getSuccess() != 1) {
                    errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                        .primitives(primitiveBatch.stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive).collect(Collectors.toList()))
                        .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata Action API reports a failed query!"))
                        .build()
                    );
                } else {
                    for (TestCompanion testCompanion : primitiveBatch) {
                        check(testCompanion, entityQueryResult);
                    }
                }
            } catch (IOException e) {
                finalNotification = NETWORK_FAILED_NOTIFICATION;
                errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                    .primitives(primitiveBatch.stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive).collect(Collectors.toList()))
                    .message(VALIDATOR_MESSAGE_MARKER + e.getMessage())
                    .build()
                );
            }
        }, this::updateBatchProgress);
    }

    /**
     * Checks an {@link OsmPrimitive} against a given {@link WbgetentitiesResult}.
     * @param tc the test companion for the {@link OsmPrimitive}
     * @param entityQueryResult the result from the Wikidata Action API
     */
    private void check(final TestCompanion tc, final WbgetentitiesResult entityQueryResult) {
        final WbgetentitiesResult.Entity entity = entityQueryResult.getEntities().get(tc.wikidataId);
        if (entityQueryResult.getMissingEntities().stream().anyMatch(it -> tc.wikidataId.equals(it.getId()))) {
            errors.add(
                AllValidationTests.WIKIDATA_ITEM_DOES_NOT_EXIST.getBuilder(this)
                    .primitives(tc.getPrimitive())
                    .message(
                        VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata item does not exist! Replace the wikidata=* tag with an existing Wikidata item or remove the Wikidata tag."),
                        I18n.marktr("Item {0} does not exist!"),
                        tc.wikidataId
                    )
                    .build()
            );
        } else if (entity == null) {
            errors.add(
                AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                    .primitives(tc.getPrimitive())
                    .message(
                        VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata Action API did not respond with all requested entities!"),
                        I18n.marktr("Item {0} is missing"),
                        tc.wikidataId
                    )
                    .build());
        } else if (!tc.wikidataId.equals(entity.getId())) {
            errors.add(
                AllValidationTests.WIKIDATA_ITEM_IS_REDIRECT.getBuilder(this)
                    .primitives(tc.getPrimitive())
                    .message(
                        VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata item is a redirect", tc.wikidataId, entity.getId()),
                        I18n.marktr("Item {0} redirects to {1}"),
                        tc.wikidataId,
                        entity.getId()
                    )
                    .fix(() -> new ChangePropertyCommand(tc.getPrimitive(), "wikidata", entity.getId()))
                    .build()
            );
        }
    }

    @Override
    protected TestCompanion prepareTestCompanion(OsmPrimitive primitive) {
        final String wikidataValue = primitive.get("wikidata");
        if (wikidataValue != null) {
            if (RegexUtil.isValidQId(wikidataValue)) {
                return new TestCompanion(primitive, wikidataValue);
            } else {
                errors.add(
                    AllValidationTests.INVALID_QID.getBuilder(this)
                        .primitives(primitive)
                        .message(
                            VALIDATOR_MESSAGE_MARKER + I18n.tr("Invalid Q-ID! Cannot exist on Wikidata."),
                            I18n.marktr("{0} is not a valid Wikidata-ID")
                        )
                        .build()
                );
            }
        }
        return null;
    }

    static final class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        final String wikidataId;
        TestCompanion(final OsmPrimitive primitive, final String wikidataId) {
            super(primitive);
            this.wikidataId = Objects.requireNonNull(wikidataId);
        }
    }
}
