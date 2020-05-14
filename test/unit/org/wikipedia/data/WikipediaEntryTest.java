// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class WikipediaEntryTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void testParseFromUrl1() {
        final Optional<WikipediaEntry> actual = WikipediaEntry.fromUrl("https://de.wikipedia.org/wiki/Österreich");
        assertThat(actual.get().article, is("Österreich"));
        assertThat(actual.get().lang, is("de"));
    }

    @Test
    public void testParseFromUrl2() {
        final Optional<WikipediaEntry> actual = WikipediaEntry.fromUrl("http://de.m.wikipedia.org/wiki/%C3%96sterreich");
        assertThat(actual.get().article, is("Österreich"));
        assertThat(actual.get().lang, is("de"));
    }

    @Test
    public void testParseFromUrl3() {
        final Optional<WikipediaEntry> actual = WikipediaEntry.fromUrl("http://de.wikipedia.org/wiki/Sternheim_%26_Emanuel");
        assertThat(actual.get().article, is("Sternheim & Emanuel"));
        assertThat(actual.get().lang, is("de"));
    }

    @Test
    public void testParseFromUrl4() {
        final Optional<WikipediaEntry> actual = WikipediaEntry.fromUrl("//de.wikipedia.org/wiki/Reichstagsgeb%C3%A4ude");
        assertThat(actual.get().article, is("Reichstagsgebäude"));
        assertThat(actual.get().lang, is("de"));
    }

    @Test
    public void testParseFromTag0() {
        final WikipediaEntry actual = WikipediaEntry.parseTag("wikipedia", "Österreich");
        assertThat(actual, nullValue());
    }

    @Test
    public void testParseFromTag1() {
        final WikipediaEntry actual = WikipediaEntry.parseTag("wikipedia", "de:Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag2() {
        final WikipediaEntry actual = WikipediaEntry.parseTag("wikipedia:de", "Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag3() {
        final WikipediaEntry actual = WikipediaEntry.parseTag("wikipedia:de", "de:Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag4() {
        final WikipediaEntry actual = WikipediaEntry.parseTag("wikipedia", "https://de.wikipedia.org/wiki/Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testGetBrowserUrl() {
        final WikipediaEntry entry = new WikipediaEntry("de", "Sternheim & Emanuel");
        assertThat(entry.getBrowserUrl().get(), is("https://de.wikipedia.org/wiki/Sternheim_%26_Emanuel"));
    }

}
