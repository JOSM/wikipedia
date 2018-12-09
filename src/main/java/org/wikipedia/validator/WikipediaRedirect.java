// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikipedia_action.WikipediaActionApiQuery;
import org.wikipedia.api.wikipedia_action.json.QueryResult;
import org.wikipedia.data.IWikipediaSite;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.OsmPrimitiveUtil;
import org.wikipedia.tools.OsmTagConstants;

public class WikipediaRedirect extends BatchProcessedTagTest<WikipediaRedirect.TestCompanion> {

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not check for all wikipedia=* tags if they redirect to another lemma.") +
            "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    public WikipediaRedirect() {
        super(
            I18n.tr("Check wikipedia=* is not a redirect"),
            I18n.tr("Make sure that the wikipedia=* article is not redirecting to another lemma")
        );
    }

    @Override
    protected TestCompanion prepareTestCompanion(OsmPrimitive primitive) {
        final String plainWikipediaValue = primitive.get(OsmTagConstants.Key.WIKIPEDIA);
        final Optional<Pair<IWikipediaSite, String>> companion = OsmPrimitiveUtil.getWikipediaValue(primitive);
        if (plainWikipediaValue != null && !companion.isPresent()) {
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                    .message(
                        VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikipedia tag has invalid format!"),
                        I18n.marktr("The value ''{0}'' is not allowed for the wikipedia=* tag"),
                        plainWikipediaValue
                    )
                    .primitives(primitive)
                    .build()
            );
        }

        return companion
            .map(it -> new TestCompanion(primitive, it.a, it.b))
            .orElse(null);
    }

    @Override
    protected void check(List<TestCompanion> allPrimitives) {
        allPrimitives.stream()
            .collect(Collectors.groupingBy(it -> it.site.getLanguageCode()))
            .forEach((langCode, primitiveList) -> {
                ListUtil.processInBatches(
                    new ArrayList<>(primitiveList.stream()
                        .collect(Collectors.groupingBy(
                            it -> it.title,
                            Collectors.mapping(BatchProcessedTagTest.TestCompanion::getPrimitive, Collectors.toList())
                        ))
                        .entrySet()
                    ),
                    50,
                    batch -> {
                        primitiveList.stream().findAny().ifPresent(any -> {
                            this.checkBatch(any.site, batch);
                        });
                    },
                    this::updateBatchProgress
                );
            });
    }

    /**
     * Check one batch containing only article titles for one Wikipedia
     * @param site the Wikimedia site for which the titles should be checked
     * @param batch a list of map entries, which map the title of an article to the list of primitives
     *     whose wikipedia=* tag points to that lemma.
     */
    private void checkBatch(final IWikipediaSite site, final List<Map.Entry<String, List<OsmPrimitive>>> batch) {
        try {
            final QueryResult.Query.Redirects redirects = ApiQueryClient.query(
                WikipediaActionApiQuery.query(site, batch.stream().map(Map.Entry::getKey).collect(Collectors.toList()))
            ).getQuery().getRedirects();
            for (Map.Entry<String, List<OsmPrimitive>> entry : batch) {
                final String redirectedTitle = redirects.resolveRedirect(entry.getKey());
                if (redirectedTitle != null && !redirectedTitle.equals(entry.getKey())) {
                    errors.add(
                        AllValidationTests.WIKIPEDIA_ARTICLE_REDIRECTS.getBuilder(this)
                            .primitives(entry.getValue())
                            .message(
                                VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikipedia article is a redirect"),
                                I18n.marktr("Wikipedia article ''{0}'' redirects to ''{1}''"),
                                entry.getKey(),
                                redirectedTitle
                            )
                            .fix(() -> {
                                // TODO: Allow the user to view either Wikipedia article
                                final int optionPaneResult = JOptionPane.showConfirmDialog(
                                    null,
                                    I18n.tr("Should the wikipedia tag be replaced with the redirect target? Make sure the meaning of the tag remains the same!\n\nBefore: wikipedia={0}:{1}\nAfter: wikipedia={0}:{2}", site.getLanguageCode(), entry.getKey(), redirectedTitle),
                                    I18n.tr("Change wikipedia tag?"),
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE
                                );
                                if (optionPaneResult == JOptionPane.YES_OPTION) {
                                    return new ChangePropertyCommand(
                                        entry.getValue(),
                                        OsmTagConstants.Key.WIKIPEDIA,
                                        site.getLanguageCode() + ':' + redirectedTitle
                                    );
                                }
                                return null;

                            })
                            .build()
                    );
                }
            }
        } catch (IOException e) {
            errors.add(
                AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                    .primitives(batch.stream().flatMap(it -> it.getValue().stream()).collect(Collectors.toList()))
                    .message(VALIDATOR_MESSAGE_MARKER + e.getMessage())
                    .build()
            );
            finalNotification = NETWORK_FAILED_NOTIFICATION;
        }
    }

    static class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        final IWikipediaSite site;
        final String title;

        TestCompanion(OsmPrimitive primitive, final IWikipediaSite site, final String title) {
            super(primitive);
            this.site = Objects.requireNonNull(site);
            this.title = Objects.requireNonNull(title);

        }
    }
}
