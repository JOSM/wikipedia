package org.wikipedia.api.wdq;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.wikipedia.testutils.JunitJupiterCompatUtil.assertThrows;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wdq.json.SparqlResult;
import org.wikipedia.testutils.ResourceFileLoader;
import org.wikipedia.tools.WikiProperties;

public class WdqApiQueryTest {

    private static final String URL_PATH = "/sparql";

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

    @Rule
    public JOSMTestRules josmRules = new JOSMTestRules().preferences().timeout(30_000);

    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() throws MalformedURLException {
        WdqApiQuery.setBaseUrl(new URL("http://localhost:" + wmRule.port() + URL_PATH));
    }

    @Test
    public void testMixed() throws IOException, URISyntaxException {
        stubWithFileContent("mixed.json");
        WikiProperties.WIKIPEDIA_LANGUAGE.put("es");
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Arrays.asList(BRIDGE_CLASS, BUILDING_CLASS), MIXED_LIST);
        verifyOneRequestTo("format=json&query=SELECT+DISTINCT+%3Fitem+%3FitemLabel+%3Fclasses+%3FclassesLabel+WHERE+%7B+VALUES+%3Fitem+%7B+wd%3AQ99236+wd%3AQ48435+wd%3AQ44440+wd%3AQ18712428+wd%3AQ494895+wd%3AQ83125+wd%3AQ54495+wd%3AQ459086+wd%3AQ507939+wd%3AQ52505+wd%3AQ201013+wd%3AQ18109819+wd%3AQ805835+wd%3AQ5459867+wd%3AQ379080+wd%3AQ3368242+%7D.+VALUES+%3Fclasses+%7B+wd%3AQ12280+wd%3AQ41176+%7D.+%3Fitem+wdt%3AP31%2Fwdt%3AP279*+%3Fsupertype.+%3Fsupertype+wdt%3AP279*+%3Fclasses.+SERVICE+wikibase%3Alabel+%7B+bd%3AserviceParam+wikibase%3Alanguage+%22es%22+%7D.+%7D");
    }

    @Test
    public void testBridges() throws IOException, URISyntaxException {
        stubWithFileContent("bridges.json");
        WikiProperties.WIKIPEDIA_LANGUAGE.put("de");
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Collections.singletonList(BRIDGE_CLASS), BRIDGE_LIST);
        verifyOneRequestTo("format=json&query=SELECT+DISTINCT+%3Fitem+%3FitemLabel+%3Fclasses+%3FclassesLabel+WHERE+%7B+VALUES+%3Fitem+%7B+wd%3AQ99236+wd%3AQ48435+wd%3AQ44440+wd%3AQ18712428+wd%3AQ494895+wd%3AQ83125+wd%3AQ54495+wd%3AQ459086+wd%3AQ507939+wd%3AQ52505+wd%3AQ201013+wd%3AQ18109819+wd%3AQ805835+wd%3AQ5459867+wd%3AQ379080+wd%3AQ3368242+%7D.+VALUES+%3Fclasses+%7B+wd%3AQ12280+%7D.+%3Fitem+wdt%3AP31%2Fwdt%3AP279*+%3Fsupertype.+%3Fsupertype+wdt%3AP279*+%3Fclasses.+SERVICE+wikibase%3Alabel+%7B+bd%3AserviceParam+wikibase%3Alanguage+%22de%22+%7D.+%7D");
    }

    @Test
    public void testBuildings() throws IOException, URISyntaxException {
        stubWithFileContent("buildings.json");
        WikiProperties.WIKIPEDIA_LANGUAGE.put("zh");
        testFindInstancesOfClassesOrTheirSubclasses(MIXED_LIST, Collections.singletonList(BUILDING_CLASS), BUILDING_LIST);
        verifyOneRequestTo("format=json&query=SELECT+DISTINCT+%3Fitem+%3FitemLabel+%3Fclasses+%3FclassesLabel+WHERE+%7B+VALUES+%3Fitem+%7B+wd%3AQ99236+wd%3AQ48435+wd%3AQ44440+wd%3AQ18712428+wd%3AQ494895+wd%3AQ83125+wd%3AQ54495+wd%3AQ459086+wd%3AQ507939+wd%3AQ52505+wd%3AQ201013+wd%3AQ18109819+wd%3AQ805835+wd%3AQ5459867+wd%3AQ379080+wd%3AQ3368242+%7D.+VALUES+%3Fclasses+%7B+wd%3AQ41176+%7D.+%3Fitem+wdt%3AP31%2Fwdt%3AP279*+%3Fsupertype.+%3Fsupertype+wdt%3AP279*+%3Fclasses.+SERVICE+wikibase%3Alabel+%7B+bd%3AserviceParam+wikibase%3Alanguage+%22zh%22+%7D.+%7D");
    }

    @Test
    public void testIllegalArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(new ArrayList<>(), Collections.singletonList("Q1"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(Collections.singletonList("Q1"), Collections.emptyList());
        });
        assertThrows(NullPointerException.class, () -> {
            WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(null, Collections.singletonList("Q1"));
        });
        assertThrows(NullPointerException.class, () -> {
            WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(Collections.singletonList("Q1"), null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(Collections.singletonList("X1"), Collections.singletonList(""));
        });
    }

    private void stubWithFileContent(final String filename) throws IOException, URISyntaxException {
        stubFor(post(URL_PATH)
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(ResourceFileLoader.getResourceBytes(WdqApiQueryTest.class, "response/findInstancesOfClassesOrTheirSubclasses/" + filename))
            )
        );
    }

    private void verifyOneRequestTo(final String urlPattern) {
        verify(1, postRequestedFor(urlEqualTo(URL_PATH)).withRequestBody(new EqualToPattern(urlPattern)));
    }

    private static void testFindInstancesOfClassesOrTheirSubclasses(final Collection<String> itemList, final Collection<String> classesList, final Collection<String> expectedResultList) throws IOException {
        final SparqlResult result = ApiQueryClient.query(WdqApiQuery.findInstancesOfClassesOrTheirSubclasses(itemList, classesList));
        for (final String expectedEntry : expectedResultList) {
            assertEquals("Entry " + expectedEntry + " not found in the result!", 1, result.getRows().stream().filter(row -> ("http://www.wikidata.org/entity/" + expectedEntry).equals(row.get(0).getValue())).count());
        }
        assertEquals(expectedResultList.size(), result.size());
    }
}
