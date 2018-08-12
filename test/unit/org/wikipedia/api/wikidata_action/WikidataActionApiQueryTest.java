// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.testutils.ResourceFileLoader;

public class WikidataActionApiQueryTest extends WikidataActionApiTestAbstract {

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

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetentitiesLabels_invalidQid() {
        WikidataActionApiQuery.wbgetentitiesLabels("X1");
    }

    @Test
    public void testApiName() {
        assertEquals("Wikidata Action API", WikidataActionApiQuery.wbgetclaims("Q1").getApiName());
        assertEquals("Wikidata Action API", WikidataActionApiQuery.sitematrix().getApiName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetclaims_invalidQid() {
        WikidataActionApiQuery.wbgetclaims("123");
    }

    @Test
    public void testWbgetentitiesQuery() {
        assertEquals(
            "format=json&utf8=1&formatversion=1&action=wbgetentities&sites=&props=&ids=Q1",
            WikidataActionApiQuery.wbgetentities(Collections.singletonList("Q1")).getQueryString()
        );
        assertEquals(
            "format=json&utf8=1&formatversion=1&action=wbgetentities&sites=&props=&ids=Q1%7CQ13%7CQ24%7CQ20150617%7CQ42%7CQ12345",
            WikidataActionApiQuery.wbgetentities(Arrays.asList("Q1", "Q13", "Q24", "Q20150617", "Q42", "Q12345")).getQueryString()
        );
    }

    @Test
    public void testWikidataForArticles1() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/dewiki_Berlin.json"));

        final WbgetentitiesResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities("dewiki", Collections.singletonList("Berlin")));

        assertEquals(1, result.getSuccess());
        assertEquals(0, result.getMissingEntities().size());
        assertEquals(1, result.getEntities().size());
        final Map.Entry<String, WbgetentitiesResult.Entity> entityEntry = result.getEntities().entrySet().iterator().next();
        assertEquals("Q64", entityEntry.getKey());
        assertEquals("item", entityEntry.getValue().getType());
        assertEquals("Q64", entityEntry.getValue().getId());
        final Collection<WbgetentitiesResult.Entity.Sitelink> sitelinks = entityEntry.getValue().getSitelinks();
        assertEquals(1, sitelinks.size());
        assertEquals("dewiki", sitelinks.iterator().next().getSite());
        assertEquals("Berlin", sitelinks.iterator().next().getTitle());

        simpleRequestVerify("format=json&utf8=1&formatversion=1&action=wbgetentities&props=sitelinks&sites=dewiki&sitefilter=dewiki&titles=Berlin");
    }

    @Test
    public void testWikidataForArticles2() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/enwiki_2entities2missing.json"));

        final WbgetentitiesResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities("enwiki", Arrays.asList("United States", "missing-article", "Great Britain", "Another missing article")));

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

        final WbgetentitiesResult.MissingEntity missing1 = result.getMissingEntities().stream().filter(it -> "missing-article".equals(it.getTitle())).findFirst().orElse(null);
        assertNull(missing1.getId());
        assertEquals("enwiki", missing1.getSite());
        final WbgetentitiesResult.MissingEntity missing2 = result.getMissingEntities().stream().filter(it -> "Another missing article".equals(it.getTitle())).findFirst().orElse(null);
        assertNull(missing2.getId());
        assertEquals("enwiki", missing2.getSite());

        simpleRequestVerify("format=json&utf8=1&formatversion=1&action=wbgetentities&props=sitelinks&sites=enwiki&sitefilter=enwiki&titles=United+States%7Cmissing-article%7CGreat+Britain%7CAnother+missing+article");
    }

    @Test
    public void testWikidataItemLabelQuery() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/labels_Q42.json"));

        final Optional<WbgetentitiesResult.Entity> result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels("Q42"));
        assertEquals(138, result.get().getLabels().size());

        assertEquals("Douglas Adams", result.get().getLabels().get("en"));
        assertEquals("Дуглас Адамс", result.get().getLabels().get("ru"));
        assertEquals("더글러스 애덤스", result.get().getLabels().get("ko"));
        assertEquals("ಡಾಗ್ಲಸ್ ಆಡಮ್ಸ್", result.get().getLabels().get("tcy"));

        simpleRequestVerify("format=json&utf8=1&formatversion=1&action=wbgetentities&props=labels|descriptions|aliases&ids=Q42");
    }

}
