// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.CheckEntityExistsResult;

public class WikidataActionApiQueryTest {

    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public JOSMTestRules josmRule = new JOSMTestRules().preferences();

    private URL oldDefaultUrl = null;

    @Before
    public void before() throws MalformedURLException {
        oldDefaultUrl = WikidataActionApiQuery.defaultUrl;
        WikidataActionApiQuery.defaultUrl = new URL("http://localhost:" + wmRule.port());
    }

    @After
    public void after() {
        WikidataActionApiQuery.defaultUrl = oldDefaultUrl;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetentities_nonQId() {
        WikidataActionApiQuery.wbgetentities(Collections.singletonList("X1"));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCheckEntityExists_nonQId2() {
        WikidataActionApiQuery.wbgetentities(Arrays.asList("Q1", "Q2", "X1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetentities_nullId() {
        WikidataActionApiQuery.wbgetentities(Arrays.asList("Q1", null, "Q3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetentities_emptyIdList() {
        WikidataActionApiQuery.wbgetentities(Collections.emptyList());
    }

    @Test
    public void testWbgetentitiesQuery() {
        assertEquals(
            "format=json&utf8=1&formatversion=1&action=wbgetentities&sites=&props=&ids=Q1",
            WikidataActionApiQuery.wbgetentities(Collections.singletonList("Q1")).getQuery()
        );
        assertEquals(
            "format=json&utf8=1&formatversion=1&action=wbgetentities&sites=&props=&ids=Q1%7CQ13%7CQ24%7CQ20150617%7CQ42%7CQ12345",
            WikidataActionApiQuery.wbgetentities(Arrays.asList("Q1", "Q13", "Q24", "Q20150617", "Q42", "Q12345")).getQuery()
        );
    }

    @Test
    public void testWikidataForArticles1() throws IOException, URISyntaxException {

        stubFor(post("/")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(getFileContentsFromResource("response/wbgetentities/dewiki:Berlin.json"))
            )
        );

        final CheckEntityExistsResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities("dewiki", Collections.singletonList("Berlin")));

        assertEquals(1, result.getSuccess());
        assertEquals(0, result.getMissingEntities().size());
        assertEquals(1, result.getEntities().size());
        final Map.Entry<String, CheckEntityExistsResult.Entity> entityEntry = result.getEntities().entrySet().iterator().next();
        assertEquals("Q64", entityEntry.getKey());
        assertEquals("item", entityEntry.getValue().getType());
        assertEquals("Q64", entityEntry.getValue().getId());
        final Collection<CheckEntityExistsResult.Entity.Sitelink> sitelinks = entityEntry.getValue().getSitelinks();
        assertEquals(1, sitelinks.size());
        assertEquals("dewiki", sitelinks.iterator().next().getSite());
        assertEquals("Berlin", sitelinks.iterator().next().getTitle());

        verify(postRequestedFor(urlEqualTo("/")).withRequestBody(new EqualToPattern("format=json&utf8=1&formatversion=1&action=wbgetentities&props=sitelinks&sites=dewiki&sitefilter=dewiki&titles=Berlin")));
    }

    @Test
    public void testWikidataForArticles2() throws IOException, URISyntaxException {
        stubFor(post("/")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(getFileContentsFromResource("response/wbgetentities/enwiki:2entities2missing.json"))
            )
        );

        final CheckEntityExistsResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities("enwiki", Arrays.asList("United States", "missing-article", "Great Britain", "Another missing article")));

        assertEquals(2, result.getEntities().size());
        assertEquals(2, result.getMissingEntities().size());

        assertEquals("Q30", result.getEntities().get("Q30").getId());
        assertEquals("item", result.getEntities().get("Q30").getType());
        assertEquals(1, result.getEntities().get("Q30").getSitelinks().size());
        assertEquals("enwiki", result.getEntities().get("Q30").getSitelinks().iterator().next().getSite());
        assertEquals("United States", result.getEntities().get("Q30").getSitelinks().iterator().next().getTitle());

        assertEquals("Q23666", result.getEntities().get("Q23666").getId());
        assertEquals("item", result.getEntities().get("Q23666").getType());
        assertEquals(1, result.getEntities().get("Q23666").getSitelinks().size());
        assertEquals("enwiki", result.getEntities().get("Q23666").getSitelinks().iterator().next().getSite());
        assertEquals("Great Britain", result.getEntities().get("Q23666").getSitelinks().iterator().next().getTitle());

        final CheckEntityExistsResult.MissingEntity missing1 = result.getMissingEntities().stream().filter(it -> "missing-article".equals(it.getTitle())).findFirst().orElse(null);
        assertNull(missing1.getId());
        assertEquals("enwiki", missing1.getSite());
        final CheckEntityExistsResult.MissingEntity missing2 = result.getMissingEntities().stream().filter(it -> "Another missing article".equals(it.getTitle())).findFirst().orElse(null);
        assertNull(missing2.getId());
        assertEquals("enwiki", missing2.getSite());

        verify(postRequestedFor(urlEqualTo("/")).withRequestBody(new EqualToPattern("format=json&utf8=1&formatversion=1&action=wbgetentities&props=sitelinks&sites=enwiki&sitefilter=enwiki&titles=United+States%7Cmissing-article%7CGreat+Britain%7CAnother+missing+article")));
    }

    private static byte[] getFileContentsFromResource(final String path) throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(WikidataActionApiQueryTest.class.getResource(path).toURI()));
    }

}
