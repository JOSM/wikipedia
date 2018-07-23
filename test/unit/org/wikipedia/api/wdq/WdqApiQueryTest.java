package org.wikipedia.api.wdq;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wdq.json.SparqlResult;

public class WdqApiQueryTest {

    // TODO: Mock API responses with WireMock

    @Rule
    public JOSMTestRules josmRules = new JOSMTestRules().preferences().timeout(30_000);

    private static final List<String> BRIDGE_LIST = Arrays.asList(
        "Q99236", /* Millau viaduct */
        "Q44440", /* Golden Gate Bridge */
        "Q83125", /* Tower Bridge */
        "Q54495", /* Sydney Harbour Bridge */
        "Q459086", /* Jungfern Bridge */
        "Q52505", /* Rialto Bridge */
        "Q18109819", /* Duge Bridge */
        "Q805835", /* Baluarte Bridge */
        "Q5459867" /* Floating Bridge */
    );
    private static final List<String> BUILDING_LIST = Arrays.asList(
        "Q48435", /* Sagrada Família */
        "Q18712428", /* Makkah Clock Royal Tower Hotel */
        "Q494895", /* Lotte World Tower */
        "Q507939", /* World One */
        "Q201013", /* Svalbard Seed Vault */
        "Q379080", /* Fort Jesus */
        "Q3368242" /* Dom Tower of Utrecht */
    );
    private static final Collection<String> MIXED_LIST = Arrays.asList(
        BRIDGE_LIST.get(0),
        BUILDING_LIST.get(0),
        BRIDGE_LIST.get(1),
        BUILDING_LIST.get(1),
        BUILDING_LIST.get(2),
        BRIDGE_LIST.get(2),
        BRIDGE_LIST.get(3),
        BRIDGE_LIST.get(4),
        BUILDING_LIST.get(3),
        BRIDGE_LIST.get(5),
        BUILDING_LIST.get(4),
        BRIDGE_LIST.get(6),
        BRIDGE_LIST.get(7),
        BRIDGE_LIST.get(8),
        BUILDING_LIST.get(5),
        BUILDING_LIST.get(6)
    );
    private static final String BRIDGE_CLASS = "Q12280";
    private static final String BUILDING_CLASS = "Q41176";

    @Test
    public void test() throws IOException {
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Arrays.asList(BRIDGE_CLASS, BUILDING_CLASS), MIXED_LIST);
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Collections.singletonList(BRIDGE_CLASS), BRIDGE_LIST);
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Collections.singletonList(BUILDING_CLASS), BUILDING_LIST);
    }

    private static void testFindInstancesOfClassesOrTheirSubclasses(final Collection<String> itemList, final Collection<String> classesList, final Collection<String> expectedResultList) throws IOException {
        final SparqlResult result = ApiQueryClient.query(WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(itemList, classesList));
        for (final String expectedEntry : expectedResultList) {
            assertEquals("Entry " + expectedEntry + " not found in the result!", 1, result.getRows().stream().filter(row -> ("http://www.wikidata.org/entity/" + expectedEntry).equals(row.get(0).getValue())).count());
        }
        assertEquals(expectedResultList.size(), result.size());
    }
}
