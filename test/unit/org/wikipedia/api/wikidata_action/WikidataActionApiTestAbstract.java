package org.wikipedia.api.wikidata_action;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.wikipedia.data.WikipediaSite;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

// Name must not end in "Test", so "Abstract" put at the end.
@WireMockTest
public abstract class WikidataActionApiTestAbstract {
    private URL oldDefaultUrl = null;

    @BeforeEach
    public void before(WireMockRuntimeInfo wmRuntimeInfo) throws MalformedURLException {
        oldDefaultUrl = setApiUrl(new URL(wmRuntimeInfo.getHttpBaseUrl()));
    }

    @AfterEach
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

    protected static WikipediaSite siteFromStub(final String code) throws IOException {
        final MappingBuilder mapping = post("/")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(new BufferedReader(new InputStreamReader(WikidataActionApiQueryTest.class.getResourceAsStream("response/sitematrix/sitematrix.json"), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n")))
            );
        stubFor(mapping);
        final WikipediaSite site = new WikipediaSite(code);
        removeStub(mapping);
        return site;
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
