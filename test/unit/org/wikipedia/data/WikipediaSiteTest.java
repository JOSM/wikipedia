// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.wikipedia.api.wikidata_action.WikidataActionApiQueryTest;
import org.wikipedia.testutils.ResourceFileLoader;

public class WikipediaSiteTest {

    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public final JOSMTestRules rules = new JOSMTestRules().preferences();

    private URL oldDefaultUrl;

    @Before
    public void before() throws IOException, URISyntaxException {
        oldDefaultUrl = WikidataActionApiQueryTest.setApiUrl(new URL("http://localhost:" + wmRule.port()));

        stubFor(post("/")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/sitematrix/sitematrix.json"))
            )
        );
    }

    @After
    public void after() {
        WikidataActionApiQueryTest.setApiUrl(oldDefaultUrl);
    }

    @Test
    public void testSites() throws IOException {
        assertEquals("https://de.wikipedia.org", new WikipediaSite("de").getSite().getUrl());
        assertEquals("https://simple.wikipedia.org", new WikipediaSite("simple").getSite().getUrl());
        assertEquals("https://be-tarask.wikipedia.org", new WikipediaSite("be-x-old").getSite().getUrl());
        assertEquals("https://cbk-zam.wikipedia.org", new WikipediaSite("cbk-zam").getSite().getUrl());
        assertEquals("https://zh-min-nan.wikipedia.org", new WikipediaSite("zh-min-nan").getSite().getUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownSite() throws IOException {
        new WikipediaSite("xy");
    }
}
