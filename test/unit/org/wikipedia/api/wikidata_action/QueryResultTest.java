package org.wikipedia.api.wikidata_action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.testutils.ResourceFileLoader;

public class QueryResultTest extends WikidataActionApiTestAbstract {
    @Test
    public void test() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/query/languages.json"));
        final Map<String, String> languages = ApiQueryClient.query(WikidataActionApiQuery.queryLanguages());
        assertEquals(445, languages.size());
        assertEquals("Deutsch", languages.get("de"));
        assertEquals("English", languages.get("en"));
        assertEquals("français", languages.get("fr"));
        assertEquals("فارسی", languages.get("fa"));
        assertEquals("עברית", languages.get("he"));
        assertEquals("адыгабзэ", languages.get("ady-cyrl"));
        assertEquals("中文（中国大陆）\u200E", languages.get("zh-cn"));
        assertFalse(languages.containsKey("xyz"));
        simpleRequestVerify("format=json&utf8=1&formatversion=1&action=query&meta=siteinfo&siprop=languages");
    }
}
