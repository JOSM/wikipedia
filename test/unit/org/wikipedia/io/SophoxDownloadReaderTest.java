// License: GPL. For details, see LICENSE file.
package org.wikipedia.io;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;

/**
 * Unit tests of {@link SophoxDownloadReader} class.
 */
class SophoxDownloadReaderTest {
    /**
     * Tests point generation
     */
    @Test
    void testPoint() {
        assertThat(SophoxDownloadReader.point(9.5, 47.16),
                is("\"Point(9.5 47.16)\"^^geo:wktLiteral"));
        assertThat(SophoxDownloadReader.boxParams(1.1, 2.2, 3.3, 4.4),
                is("\nbd:serviceParam wikibase:cornerWest \"Point(1.1 2.2)\"^^geo:wktLiteral." +
                        "\nbd:serviceParam wikibase:cornerEast \"Point(3.3 4.4)\"^^geo:wktLiteral.\n"));
    }

    /**
     * Tests server response parsing
     * @throws UnsupportedEncodingException if an error occurs
     */
    @Test
    void testIdParsing() throws UnsupportedEncodingException {
        String json = String.join("\n",
                "{\"results\":{\"bindings\":[",
                "{\"a\":{\"type\": \"uri\", \"value\": \"https://www.openstreetmap.org/node/12345\"}},",
                "{\"a\":{\"type\": \"uri\", \"value\": \"https://www.openstreetmap.org/way/1234512345\"}},",
                "{\"a\":{\"type\": \"uri\", \"value\": \"https://www.openstreetmap.org/relation/98765\"}}",
                "]}}"
        );

        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8.name()));
        List<PrimitiveId> actual = SophoxDownloadReader.getPrimitiveIds(stream);

        List<PrimitiveId> expected = Arrays.asList(new PrimitiveId[]{
                new SimplePrimitiveId(12345, OsmPrimitiveType.NODE),
                new SimplePrimitiveId(1234512345, OsmPrimitiveType.WAY),
                new SimplePrimitiveId(98765, OsmPrimitiveType.RELATION),
        });

        assertThat(actual, is(expected));
    }
}
