package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wdq.WdqApiQuery;
import org.wikipedia.api.wdq.json.SparqlResult;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.OsmTagConstants;
import org.wikipedia.tools.RegexUtil;
import org.wikipedia.tools.WikiProperties;

public class UnusualWikidataClasses extends BatchProcessedTagTest<UnusualWikidataClasses.TestCompanion> {
    private static final String BRAND_QID = "Q167270";

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
        final String wikidataValue = primitive.get(OsmTagConstants.Key.WIKIDATA);
        if (RegexUtil.isValidQId(wikidataValue)) {
            return new TestCompanion(primitive, wikidataValue);
        }
        return null;
    }

    @Override
    protected void check(List<TestCompanion> allPrimitives) {
        ListUtil.processInBatches(
            // group the same wikidata values to reduce number of queries
            new ArrayList<>(allPrimitives.stream().collect(Collectors.groupingBy(it -> it.wikidataValue)).entrySet()),
            50,
            batch -> {
                try {
                    final SparqlResult result = ApiQueryClient.query(WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(
                        batch.stream().map(Map.Entry::getKey).collect(Collectors.toList()),
                        WikiProperties.WIKIDATA_VALIDATOR_UNUSUAL_CLASSES.get()
                    ));
                    for (final List<SparqlResult.Results.Entry> row : result.getRows()) {
                        final String itemUrl = row.get(0).getValue();
                        final String itemQId = itemUrl.substring(itemUrl.lastIndexOf('/') >= 0 ? itemUrl.lastIndexOf('/') + 1 : 0);
                        final String classUrl = row.get(2).getValue();
                        final String classQId = classUrl.substring(classUrl.lastIndexOf('/') >= 0 ? classUrl.lastIndexOf('/') + 1 : 0);

                        final Collection<OsmPrimitive> primitives = batch.stream()
                            .filter(it -> itemQId.equals(it.getKey()))
                            .flatMap(it -> it.getValue().stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive))
                            .collect(Collectors.toList());
                        if (!primitives.isEmpty()) {
                            buildTestError(primitives, itemQId, classQId, row);
                        }
                    }
                } catch (IOException e) {
                    errors.add(
                        AllValidationTests.API_REQUEST_FAILED.getBuilder(this)
                            .primitives(batch.stream().flatMap(it -> it.getValue().stream().map(BatchProcessedTagTest.TestCompanion::getPrimitive)).collect(Collectors.toList()))
                            .message(VALIDATOR_MESSAGE_MARKER + e.getMessage())
                            .build()
                    );
                    finalNotification = NETWORK_FAILED_NOTIFICATION;
                }
            }
        );
    }

    private void buildTestError(final Collection<OsmPrimitive> primitives, final String itemQId, final String classQId, final List<SparqlResult.Results.Entry> row) {
        final TestError.Builder errBuilder = AllValidationTests.WIKIDATA_TAG_HAS_UNUSUAL_TYPE.getBuilder(this)
            .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikidata value is of unusual type for the wikidata=* tag on OSM objects"),
                I18n.marktr("{0} is an instance of {1} (or any subclass thereof)"),
                row.get(1).getValue() + " (" + itemQId + ")",
                row.get(3).getValue() + " (" + classQId + ")"
            )
            .primitives(primitives);

        // Add autofix if item is a brand, move tag value to brand:wikidata=* in that case.
        if (BRAND_QID.equals(classQId)) {
            final Map<String, List<OsmPrimitive>> prevBrands = primitives.stream()
                .collect(Collectors.groupingBy(it -> Optional.ofNullable(it.get(OsmTagConstants.Key.BRAND_WIKIDATA)).orElse("")));

            final List<Command> commands = new ArrayList<>();
            for (final Map.Entry<String, List<OsmPrimitive>> prevBrand : prevBrands.entrySet()) {
                final Map<String, String> tags = new HashMap<>();
                tags.put(OsmTagConstants.Key.WIKIDATA, null);
                final String prevBrandValue = prevBrand.getKey();
                final String newBrandValue;
                if (prevBrandValue == null || "".equals(prevBrandValue)) {
                    newBrandValue = itemQId;
                } else {
                    if (!RegexUtil.isValidMultiQId(prevBrandValue)) {
                        errors.add(
                            AllValidationTests.INVALID_BRAND_WIKIDATA_TAG_FORMAT.getBuilder(this)
                                .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("The value of tag brand:wikidata=* has an invalid format!"))
                                .primitives(prevBrand.getValue())
                                .build()
                        );
                        newBrandValue = null;
                    } else if (Arrays.asList(prevBrandValue.split(";")).contains(itemQId)) {
                        newBrandValue = prevBrandValue;
                    } else {
                        newBrandValue = prevBrandValue + ";" + itemQId;
                    }
                }
                if (newBrandValue != null) {
                    if(!newBrandValue.equals(prevBrandValue)) {
                        tags.put(OsmTagConstants.Key.BRAND_WIKIDATA, newBrandValue);
                    }
                    commands.add(new ChangePropertyCommand(prevBrand.getValue(), tags));
                }
            }
            if (commands.size() == 1) {
                errBuilder.fix(() -> commands.get(0));
            } else if (!commands.isEmpty()) {
                errBuilder.fix(() -> new SequenceCommand(I18n.tr("Move wikidata=* tags to tag brand:wikidata=*, when the value points to a brand"), commands));
            }
        }

        errors.add(errBuilder.build());
    }

    static class TestCompanion extends BatchProcessedTagTest.TestCompanion {
        private final String wikidataValue;

        TestCompanion(final OsmPrimitive primitive, final String wikidataValue) {
            super(primitive);
            this.wikidataValue = wikidataValue;
        }
    }
}
