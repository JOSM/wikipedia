// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.wikipedia.api.wikidata_action.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiUrl;
import org.wikipedia.api.wikidata_action.json.CheckEntityExistsResult;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;
import org.wikipedia.tools.RegexUtil;

/**
 * Checks if for the wikidata=* tag on an {@link OsmPrimitive} a Wikidata item really exists.
 * This check requires a working internet connection, because it queries the Wikidata Action API.
 */
public class WikidataItemExists extends Test.TagTest {

    private static final int CHUNK_SIZE = 50;
    private List<OsmPrimitive> primitivesForChunks = new ArrayList<>();


    public WikidataItemExists() {
        //super("wikipedia=* is interwiki link of wikidata=*", "make sure that the wikipedia=* article is connected to the wikidata=* item");
        super(I18n.tr("wikidata=* item exists"), I18n.tr("Make sure the Wikidata item for the Q-ID in the wikidata=* tag actually exists"));
    }

    /**
     * Checks one "chunk" of {@link OsmPrimitive}s at a time.
     * The checks that are done here are requiring API requests and thus are grouped into these chunks.
     * @param primitives the primitives that belong to the current chunk and should be checked now.
     * @return {@code false} if there were any issues with downloading or decoding the API response, {@code true} otherwise
     */
    private boolean checkChunk(final List<? extends OsmPrimitive> primitives) {
        boolean result = true;
        final List<String> qIds = primitives.stream().map(it -> {
            final String wdValue = it.get("wikidata");
            return RegexUtil.isValidQId(wdValue) ? wdValue : null;
        }).collect(Collectors.toList());
        if (qIds.stream().anyMatch(Objects::nonNull)) {
            try {
                final URL url = WikidataActionApiUrl.checkEntityExistsUrl(qIds.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                final CheckEntityExistsResult entityQueryResult = ApiQueryClient.query(url, SerializationSchema.WBGETENTITIES);
                if (entityQueryResult.getSuccess() != 1) {
                    errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this).primitives(new ArrayList<>(primitives)).message(VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata Action API reports a failed query!")).build());
                } else {
                    for (int i = 0; i < Math.min(qIds.size(), primitives.size()); i++) {
                        final OsmPrimitive primitive = primitives.get(i);
                        final String qId = qIds.get(i);
                        check(primitive, qId, entityQueryResult);
                    }
                }
            } catch (IOException e) {
                result = false;
                errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this).primitives(new ArrayList<>(primitives)).message(VALIDATOR_MESSAGE_MARKER + e.getMessage()).build());
            }
        }
        return result;
    }

    /**
     * Checks an {@link OsmPrimitive} against a given {@link CheckEntityExistsResult}.
     * @param primitive the primitive to check
     * @param qId the Wikidata ID from the tags of the given {@link OsmPrimitive}
     * @param entityQueryResult the result from the Wikidata Action API
     */
    private void check(final OsmPrimitive primitive, final String qId, final CheckEntityExistsResult entityQueryResult) {
        if (qId != null) {
            final CheckEntityExistsResult.Entity entity = entityQueryResult.getEntities().get(qId);
            if (entity == null) {
                errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this).primitives(primitive).message(VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata Action API did not respond with all requested entities!"), I18n.marktr("Item {0} is missing"), qId).build());
            } else if (!qId.equals(entity.getId())) {
                errors.add(
                    AllValidationTests.WIKIDATA_ITEM_IS_REDIRECT.getBuilder(this)
                        .primitives(primitive)
                        .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("The Wikidata item is a redirect", qId, entity.getId()), I18n.marktr("Item {0} redirects to {1}"), qId, entity.getId())
                        .fix(() -> new ChangePropertyCommand(primitive, "wikidata", entity.getId()))
                        .build()
                );
            } else if (entity.getType() == null) {
                errors.add(
                    AllValidationTests.WIKIDATA_ITEM_DOES_NOT_EXIST.getBuilder(this)
                        .primitives(primitive)
                        .message(I18n.tr("The Wikidata item does not exist! Replace the wikidata=* tag with an existing Wikidata item or remove the Wikidata tag."), I18n.marktr("Item {0} does not exist!"), qId)
                        .build()
                );
            }
        }
    }

    @Override
    public void check(OsmPrimitive osmPrimitive) {
        final String wdValue = osmPrimitive.get("wikidata");
        if (wdValue != null) {
            if (RegexUtil.isValidQId(wdValue)) {
                primitivesForChunks.add(osmPrimitive);
            } else {
                errors.add(
                    AllValidationTests.INVALID_QID.getBuilder(this)
                        .primitives(osmPrimitive)
                        .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("Invalid Q-ID! Cannot exist on Wikidata."), I18n.marktr("{0} is not a valid Wikidata-ID"))
                        .build()
                );
            }
        }
    }

    @Override
    public void endTest() {
        boolean fullSuccess = true;

        final int numPrimitives = primitivesForChunks.size();
        final int numChunks = numPrimitives / CHUNK_SIZE + (numPrimitives % CHUNK_SIZE == 0 ? 0 : 1);
        for (int chunkIndex = 0; chunkIndex * CHUNK_SIZE < numPrimitives; chunkIndex++) {
            progressMonitor.setExtraText(I18n.tr("(chunk {0}/{1} of {2} items)", chunkIndex + 1, numChunks, numPrimitives));
            fullSuccess &= checkChunk(primitivesForChunks.subList(chunkIndex * CHUNK_SIZE, Math.min(primitivesForChunks.size(), (chunkIndex + 1) * CHUNK_SIZE)));
        }

        if (!fullSuccess) {
            new Notification(
                I18n.tr("Could not validate all wikidata=* tags over the internet.") + "\n" +
                (ValidatorPrefHelper.PREF_OTHER.get()
                    ? I18n.tr("See the validator messages of the category ''Other'' for more details.")
                    : I18n.tr("Turn on the informational level validator messages in the preferences to see more details.")
                )
            )
                .setIcon(ImageProvider.get("dialogs/wikipedia"))
                .show();
        }

        primitivesForChunks.clear();
        super.endTest();
    }
}