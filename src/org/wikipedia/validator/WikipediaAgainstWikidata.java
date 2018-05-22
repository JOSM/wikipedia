// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.api.wikidata_action.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiUrl;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.RegexUtil;

public class WikipediaAgainstWikidata extends BatchProcessedTagTest<WikipediaAgainstWikidata.TestCompanion> {

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not check for all wikipedia=* tags if they match the wikidata=* tag.") + "\n" +
            (ValidatorPrefHelper.PREF_OTHER.get()
                ? I18n.tr("See the validator messages of the category ''Other'' for more details.")
                : I18n.tr("Turn on the informational level validator messages in the preferences to see more details.")
            )
    ).setIcon(ImageProvider.get("dialogs/wikipedia"));

    public WikipediaAgainstWikidata() {
        super("Check wikipedia=* is interwiki link of wikidata=*", "make sure that the wikipedia=* article is connected to the wikidata=* item");
    }

    @Override
    protected TestCompanion prepareTestCompanion(OsmPrimitive primitive) {
        final String wikipediaValue = primitive.get("wikipedia");
        final String wikidataValue = primitive.get("wikidata");
        if (wikipediaValue != null && RegexUtil.isValidQId(wikidataValue)) {
            final Matcher wpMatcher = RegexUtil.WIKIPEDIA_TAG_VALUE_PATTERN.matcher(wikipediaValue);
            if (wpMatcher.matches()) {
                return new TestCompanion(primitive, wpMatcher.group(1), wpMatcher.group(2), wikidataValue);
            }
        }
        return null;
    }

    @Override
    protected void check(List<TestCompanion> allPrimitives) {
        allPrimitives.stream()
            .collect(Collectors.groupingBy(it -> it.language)) // Group by wiki-language
            .forEach((language, primitiveList) -> {
                ListUtil.processInBatches(
                    primitiveList,
                    50,
                    primitiveBatch -> {
                        checkBatch(language, primitiveBatch);
                    },
                    this::updateBatchProgress
                );
            });
    }

    private void checkBatch(final String language, final List<TestCompanion> primitiveBatch) {
        try {
            ApiQueryClient.query(
                WikidataActionApiUrl.getEntityForSitelink(language + "wiki", primitiveBatch.stream().map(it -> it.title).collect(Collectors.toList())),
                SerializationSchema.WBGETENTITIES
            ).getEntities().values().stream()
                .flatMap(entity -> entity.getSitelinks().isPresent() ? entity.getSitelinks().get().stream().map(it -> Pair.create(it, entity.getId())) : Stream.empty())
                .forEach(sitelinkAndQId -> {
                    Optional<TestCompanion> curTestCompanion = primitiveBatch.stream().filter(it -> sitelinkAndQId.a.getTitle().equals(it.title)).findAny();
                    if (curTestCompanion.isPresent()) {
                        if (!curTestCompanion.get().qId.equals(sitelinkAndQId.b)) {
                            errors.add(AllValidationTests.WIKIDATA_ITEM_NOT_MATCHING_WIKIPEDIA.getBuilder(this)
                                .primitives(curTestCompanion.get().getPrimitive())
                                .message(
                                    AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikidata item and Wikipedia article do not match!"),
                                    I18n.marktr("Wikidata item {0} is not associated with Wikipedia article {1} ({2})"),
                                    curTestCompanion.get().qId,
                                    sitelinkAndQId.a.getTitle(),
                                    sitelinkAndQId.b
                                ).build());
                        }
                    }
                });
        } catch (IOException e) {
            errors.add(
                AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                    .primitives(primitiveBatch.stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive).collect(Collectors.toList()))
                    .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + e.getMessage())
                    .build()
            );
            finalNotification = NETWORK_FAILED_NOTIFICATION;
        }
    }

    static class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        final String language;
        final String title;
        final String qId;
        private TestCompanion(final OsmPrimitive primitive, String language, String title, final String qId) {
            super(primitive);
            this.language = Objects.requireNonNull(language);
            this.title = Objects.requireNonNull(title);
            this.qId = Objects.requireNonNull(qId);
        }
    }
}
