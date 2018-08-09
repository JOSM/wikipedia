// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikipedia_action;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikipedia_action.json.QueryResult;
import org.wikipedia.data.IWikipediaSite;
import org.wikipedia.testutils.ResourceFileLoader;

public class WikipediaActionApiQueryTest {

    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public JOSMTestRules josmRule = new JOSMTestRules().preferences();

    @Test
    public void test() throws IOException, URISyntaxException {
        stubFor(post("/w/api.php")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ResourceFileLoader.getResourceBytes(WikipediaActionApiQueryTest.class, "response/query/en_US-USA-Universe.json"))
            )
        );

        final QueryResult result = ApiQueryClient.query(
            WikipediaActionApiQuery.query(new WikiSiteMock("en"), Arrays.asList("US", "USA", "Universe"))
        );

        assertEquals("United States", result.getQuery().getRedirects().resolveRedirect("USA"));
        assertEquals("United States", result.getQuery().getRedirects().resolveRedirect("US"));
        assertEquals("United States", result.getQuery().getRedirects().resolveRedirect("United States"));
        assertEquals("Universe", result.getQuery().getRedirects().resolveRedirect("Universe"));
        assertEquals("non-existent-title", result.getQuery().getRedirects().resolveRedirect("non-existent-title"));

        assertEquals(2, result.getQuery().getPages().size());
        assertTrue(result.getQuery().getPages().stream().anyMatch(it -> "Universe".equals(it.getTitle())));
        assertTrue(result.getQuery().getPages().stream().anyMatch(it -> "United States".equals(it.getTitle())));

        verify(postRequestedFor(urlEqualTo("/w/api.php"))
            .withRequestBody(new EqualToPattern("format=json&utf8=1&formatversion=1&action=query&redirects=1&titles=US%7CUSA%7CUniverse")));
    }

    private class WikiSiteMock implements IWikipediaSite {
        private final String langCode;

        private WikiSiteMock(final String langCode) {
            this.langCode = langCode;
        }

        @Override
        public SitematrixResult.Sitematrix.Site getSite() {
            return new SitematrixResult.Sitematrix.Site("http://localhost:" + wmRule.port(), "dbname", "code", "false", "Wikipedia");
        }

        @Override
        public String getLanguageCode() {
            return langCode;
        }
    }
}
