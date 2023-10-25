// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikipedia_action;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikipedia_action.json.QueryResult;
import org.wikipedia.data.IWikipediaSite;
import org.wikipedia.testutils.ResourceFileLoader;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

@WireMockTest
class WikipediaActionApiQueryTest {

    @Test
    void test(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, URISyntaxException {
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
            WikipediaActionApiQuery.query(new WikiSiteMock(wmRuntimeInfo, "en").getSite(), Arrays.asList("US", "USA", "Universe", "united states"))
        );

        assertEquals("United States", result.getQuery().resolveRedirect("USA"));
        assertEquals("United States", result.getQuery().resolveRedirect("US"));
        assertEquals("United States", result.getQuery().resolveRedirect("United States"));
        assertEquals("United States", result.getQuery().resolveRedirect("united states"));
        assertEquals("Universe", result.getQuery().resolveRedirect("Universe"));
        assertEquals("non-existent-title", result.getQuery().resolveRedirect("non-existent-title"));

        assertEquals(2, result.getQuery().getPages().size());
        assertTrue(result.getQuery().getPages().stream().anyMatch(it -> "Universe".equals(it.getTitle())));
        assertTrue(result.getQuery().getPages().stream().anyMatch(it -> "United States".equals(it.getTitle())));

        verify(postRequestedFor(urlEqualTo("/w/api.php"))
            .withRequestBody(new EqualToPattern("action=query&format=json&formatversion=2&redirects=1&titles=US%7CUSA%7CUniverse%7Cunited+states&utf8=1")));
    }

    private static class WikiSiteMock implements IWikipediaSite {
        private final String langCode;
        private final String url;

        private WikiSiteMock(WireMockRuntimeInfo wmRuntimeInfo, final String langCode) {
            this.langCode = langCode;
            this.url = wmRuntimeInfo.getHttpBaseUrl();
        }

        @Override
        public SitematrixResult.Sitematrix.Site getSite() {
            return new SitematrixResult.Sitematrix.Site(this.url, "dbname", "code", "false", "Wikipedia");
        }

        @Override
        public String getLanguageCode() {
            return langCode;
        }
    }
}
