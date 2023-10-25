package org.wikipedia.api.wikidata_action;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.testutils.ResourceFileLoader;

class QueryResultTest extends WikidataActionApiTestAbstract {
    @Test
    void test() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/query/languages.json"));
        final Map<String, String> languages = ApiQueryClient.query(WikidataActionApiQuery.queryLanguages());
        assertEquals(456, languages.size());
        assertEquals("Deutsch", languages.get("de"));
        assertEquals("English", languages.get("en"));
        assertEquals("français", languages.get("fr"));
        assertEquals("فارسی", languages.get("fa"));
        assertEquals("עברית", languages.get("he"));
        assertEquals("адыгабзэ", languages.get("ady-cyrl"));
        assertEquals("中文（中国大陆）‎", languages.get("zh-cn"));
        assertFalse(languages.containsKey("xyz"));
        simpleRequestVerify("action=query&format=json&formatversion=2&meta=siteinfo&siprop=languages&utf8=1");
    }
}
