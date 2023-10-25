// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WikipediaEntryTest {
    static Stream<Arguments> testParseFromUrl() {
        return Stream.of(Arguments.of("https://de.wikipedia.org/wiki/Österreich", "Österreich", "de"),
            Arguments.of("http://de.m.wikipedia.org/wiki/%C3%96sterreich", "Österreich", "de"),
            Arguments.of("http://de.wikipedia.org/wiki/Sternheim_%26_Emanuel", "Sternheim & Emanuel", "de"),
            Arguments.of("//de.wikipedia.org/wiki/Reichstagsgeb%C3%A4ude", "Reichstagsgebäude", "de"));
    }

    @ParameterizedTest
    @MethodSource
    void testParseFromUrl(String url, String article, String lang) {
        final WikipediaEntry actual = WikipediaEntry.fromUrl(url).orElseThrow(AssertionError::new);
        assertEquals(article, actual.article);
        assertEquals(lang, actual.lang);
    }

    static Stream<Arguments> testParseFromTag() {
        return Stream.of(Arguments.of("wikipedia", "Österreich", null, null),
            Arguments.of("wikipedia", "de:Österreich", "Österreich", "de"),
            Arguments.of("wikipedia:de", "Österreich", "Österreich", "de"),
            Arguments.of("wikipedia:de", "de:Österreich", "Österreich", "de"),
            Arguments.of("wikipedia", "https://de.wikipedia.org/wiki/Österreich", "Österreich", "de"));
    }

    @ParameterizedTest
    @MethodSource
    void testParseFromTag(String key, String value, String article, String lang) {
        final WikipediaEntry actual = WikipediaEntry.parseTag(key, value);
        if (article == null && lang == null) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
            assertEquals(article, actual.article);
            assertEquals(lang, actual.lang);
        }
    }

    @Test
    void testGetBrowserUrl() {
        final WikipediaEntry entry = new WikipediaEntry("de", "Sternheim & Emanuel");
        assertThat(entry.getBrowserUrl().get(), is("https://de.wikipedia.org/wiki/Sternheim_%26_Emanuel"));
    }

}
