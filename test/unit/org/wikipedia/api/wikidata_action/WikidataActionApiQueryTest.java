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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.wikidata_action.json.CheckEntityExistsResult;

public class WikidataActionApiQueryTest {

    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public JOSMTestRules josmRule = new JOSMTestRules().preferences();

    @Before
    public void before() throws MalformedURLException {
        WikidataActionApiQuery.defaultUrl = new URL("http://localhost:" + wmRule.port());
    }

    @Test
    public void test() throws IOException, URISyntaxException {

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

    private byte[] getFileContentsFromResource(final String path) throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(WikidataActionApiQueryTest.class.getResource(path).toURI()));
    }

}
