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
import org.wikipedia.data.WikipediaSite;
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
        assertEquals("Wikidata Action API", WikidataActionApiQuery.wbgetentitiesClaims("Q1").getApiName());
        assertEquals("Wikidata Action API", WikidataActionApiQuery.sitematrix().getApiName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWbgetclaims_invalidQid() {
        WikidataActionApiQuery.wbgetentitiesClaims("123");
    }

    @Test
    public void testWbgetentitiesQuery() {
        assertEquals(
            "action=wbgetentities&format=json&formatversion=2&ids=Q1&props=&sites=&utf8=1",
            WikidataActionApiQuery.wbgetentities(Collections.singletonList("Q1")).getQueryString().toString()
        );
        assertEquals(
            "action=wbgetentities&format=json&formatversion=2&ids=%1FQ1%1FQ12345%1FQ13%1FQ20150617%1FQ24%1FQ42&props=&sites=&utf8=1",
            WikidataActionApiQuery.wbgetentities(Arrays.asList("Q1", "Q13", "Q24", "Q20150617", "Q42", "Q12345")).getQueryString().toString()
        );
    }

    @Test
    public void testWikidataForArticles1() throws IOException, URISyntaxException {
        final WikipediaSite site = siteFromStub("de");
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/dewiki_Berlin.json"));

        final WbgetentitiesResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities(site.getSite(), Collections.singletonList("Berlin")));

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

        simpleRequestVerify("action=wbgetentities&format=json&formatversion=2&props=sitelinks&sitefilter=dewiki&sites=dewiki&titles=Berlin&utf8=1");
    }

    @Test
    public void testWikidataForArticles2() throws IOException, URISyntaxException {
        final WikipediaSite site = siteFromStub("en");
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/enwiki_2entities2missing.json"));

        final WbgetentitiesResult result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentities(site.getSite(), Arrays.asList("United States", "missing-article", "Great Britain", "Another missing article")));

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

        simpleRequestVerify("action=wbgetentities&format=json&formatversion=2&props=sitelinks&sitefilter=enwiki&sites=enwiki&titles=%1FAnother+missing+article%1FGreat+Britain%1FUnited+States%1Fmissing-article&utf8=1");
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

        simpleRequestVerify("action=wbgetentities&format=json&formatversion=2&ids=Q42&props=aliases%7Cdescriptions%7Clabels&utf8=1");
    }

}
