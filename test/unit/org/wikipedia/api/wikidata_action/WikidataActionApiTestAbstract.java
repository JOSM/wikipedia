package org.wikipedia.api.wikidata_action;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.openstreetmap.josm.testutils.JOSMTestRules;

// Name must not end in "Test", so "Abstract" put at the end.
public abstract class WikidataActionApiTestAbstract {
    @Rule
    public WireMockRule wmRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public JOSMTestRules josmRule = new JOSMTestRules().preferences();

    private URL oldDefaultUrl = null;

    @Before
    public void before() throws MalformedURLException {
        oldDefaultUrl = setApiUrl(new URL("http://localhost:" + wmRule.port()));
    }

    @After
    public void after() {
        setApiUrl(oldDefaultUrl);
    }

    /**
     * Sets {@link WikidataActionApiQuery#defaultUrl} to the supplied URL
     * @param url the new URL
     * @return the URL to which {@link WikidataActionApiQuery#defaultUrl} was set before
     */
    private static URL setApiUrl(final URL url) {
        final URL prevURL = WikidataActionApiQuery.defaultUrl;
        WikidataActionApiQuery.defaultUrl = url;
        return prevURL;
    }

    protected static void simpleJsonStub(final byte[] bytes) {
        stubFor(post("/")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(bytes)
            )
        );
    }

    protected static void simpleRequestVerify(final String expectedQueryString) {
        verify(postRequestedFor(urlEqualTo("/")).withRequestBody(new EqualToPattern(expectedQueryString)));
    }
}
