// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class WikidataActionApiUrlTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCheckEntityExists_nonQId() {
        WikidataActionApiUrl.checkEntityExistsUrl(Collections.singletonList("X1"));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCheckEntityExists_nonQId2() {
        WikidataActionApiUrl.checkEntityExistsUrl(Arrays.asList("Q1", "Q2", "X1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckEntityExists_nullId() {
        WikidataActionApiUrl.checkEntityExistsUrl(Arrays.asList("Q1", null, "Q3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckEntityExists_emptyIdList() {
        WikidataActionApiUrl.checkEntityExistsUrl(Collections.emptyList());
    }

    @Test
    public void testCheckEntityExistsUrl() {
        assertEquals(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&sites=&props=&ids=Q1",
            WikidataActionApiUrl.checkEntityExistsUrl(Collections.singletonList("Q1")).toString()
        );
        assertEquals(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&sites=&props=&ids=Q1%7CQ42",
            WikidataActionApiUrl.checkEntityExistsUrl(Arrays.asList("Q1", "Q42")).toString()
        );
        assertEquals(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&sites=&props=&ids=Q1%7CQ42%7CQ12345",
            WikidataActionApiUrl.checkEntityExistsUrl(Arrays.asList("Q1", "Q42", "Q12345")).toString()
        );
        assertEquals(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&sites=&props=&ids=Q1%7CQ13%7CQ24%7CQ20150617%7CQ42%7CQ12345",
            WikidataActionApiUrl.checkEntityExistsUrl(Arrays.asList("Q1", "Q13", "Q24", "Q20150617", "Q42", "Q12345")).toString()
        );
    }
}
