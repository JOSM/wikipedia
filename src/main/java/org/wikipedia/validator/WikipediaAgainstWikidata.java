// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.RegexUtil;

public class WikipediaAgainstWikidata extends BatchProcessedTagTest<WikipediaAgainstWikidata.TestCompanion> {

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not check for all wikipedia=* tags if they match the wikidata=* tag.") +
        "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    public WikipediaAgainstWikidata() {
        super(
            I18n.tr("Check wikipedia=* is interwiki link of wikidata=*"),
            I18n.tr("Makes sure that the wikipedia=* article is connected to the wikidata=* item")
        );
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
            final Map<String, WbgetentitiesResult.Entity.Sitelink> sitelinks = ApiQueryClient
                .query(WikidataActionApiQuery.wbgetentities(
                    language + "wiki",
                    primitiveBatch.stream().map(it -> it.title).collect(Collectors.toList())
                ))
                .getEntities().values().stream()
                    .flatMap(entity -> entity.getSitelinks().stream().map(it -> Pair.create(entity.getId(), it)))
                    .collect(Collectors.toMap(it -> it.a, it -> it.b));
            primitiveBatch.forEach(tc -> {
                if (!sitelinks.containsKey(tc.qId) || !tc.title.equals(sitelinks.get(tc.qId).getTitle())) {
                    errors.add(AllValidationTests.WIKIDATA_ITEM_NOT_MATCHING_WIKIPEDIA.getBuilder(this)
                        .primitives(tc.getPrimitive())
                        .message(
                            VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikidata item and Wikipedia article do not match!"),
                            I18n.marktr("Wikidata item {0} is not associated with Wikipedia article {1} ({2})"),
                            tc.qId,
                            tc.language + ':' + tc.title,
                            sitelinks.entrySet().stream()
                                .filter(it -> tc.title.equals(it.getValue().getTitle()))
                                .findAny()
                                .map(Map.Entry::getKey)
                                .orElse(I18n.tr("has no Q-ID"))
                        ).build()
                    );
                }
            });
        } catch (IOException e) {
            errors.add(
                AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                    .primitives(primitiveBatch.stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive).collect(Collectors.toList()))
                    .message(VALIDATOR_MESSAGE_MARKER + e.getMessage())
                    .build()
            );
            finalNotification = NETWORK_FAILED_NOTIFICATION;
        }
    }

    static class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        final String language; // TODO: Use WikipediaSite here to verify a wiki in that language actually exists
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
