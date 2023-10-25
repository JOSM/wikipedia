// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.wikipedia.api.wikidata_action.WikidataActionApiQueryTest;
import org.wikipedia.api.wikidata_action.WikidataActionApiTestAbstract;
import org.wikipedia.testutils.ResourceFileLoader;

class WikipediaSiteTest extends WikidataActionApiTestAbstract {

    @Test
    void testSites() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/sitematrix/sitematrix.json"));

        assertEquals("https://de.wikipedia.org", new WikipediaSite("de").getSite().getUrl());
        assertEquals("https://simple.wikipedia.org", new WikipediaSite("simple").getSite().getUrl());
        assertEquals("https://be-tarask.wikipedia.org", new WikipediaSite("be-x-old").getSite().getUrl());
        assertEquals("https://cbk-zam.wikipedia.org", new WikipediaSite("cbk-zam").getSite().getUrl());
        assertEquals("https://zh-min-nan.wikipedia.org", new WikipediaSite("zh-min-nan").getSite().getUrl());
    }

    @Test
    void testUnknownSite() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/sitematrix/sitematrix.json"));
        assertThrows(IllegalArgumentException.class, () -> new WikipediaSite("xy"));
    }
}
