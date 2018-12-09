package org.wikipedia.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.data.IWikipediaSite;
import org.wikipedia.data.WikipediaSite;
import org.wikipedia.tools.OsmPrimitiveUtil;

public class OsmPrimitiveUtilTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void testWikidata() {
        assertFalse(OsmPrimitiveUtil.getWikidataValue(new Node()).isPresent());

        final Node wdNode = new Node();
        wdNode.put("wikidata", "Q1");
        assertEquals("Q1", OsmPrimitiveUtil.getWikidataValue(wdNode).get());
        assertFalse(OsmPrimitiveUtil.getWikipediaValue(wdNode).isPresent());
    }

    @Test
    public void testWikipedia() throws IOException {
        assertFalse(OsmPrimitiveUtil.getWikipediaValue(new Relation()).isPresent());

        final Way wpWay = new Way();
        wpWay.put("wikipedia", "en:London");
        final Optional<Pair<IWikipediaSite, String>> result = OsmPrimitiveUtil.getWikipediaValue(wpWay);
        assertTrue(result.isPresent());
        assertEquals(new WikipediaSite("en").getLanguageCode(), result.get().a.getLanguageCode());
        assertNotNull( result.get().a.getSite());
        assertEquals("London", result.get().b);
    }

    @Test
    public void testWithNonExistentWikipediaSite() {
        final Way wpWay = new Way();
        wpWay.put("wikipedia", "xx:Wikipedia");
        assertFalse(OsmPrimitiveUtil.getWikipediaValue(wpWay).isPresent());
    }

}
