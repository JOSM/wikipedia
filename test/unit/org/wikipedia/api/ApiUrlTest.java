// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

class ApiUrlTest {

    @Test
    void testMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> ApiUrl.url("malformedURL"));
    }

    @Test
    void testUrl() throws MalformedURLException {
        assertEquals(new URL("https://example.org"), ApiUrl.url("https://example.org"));
        assertEquals(new URL("https://example.org/abc"), ApiUrl.url("https://example.org/abc"));
        assertEquals(new URL("https://example.org/abc/def/ghi/jkl/mno"), ApiUrl.url("https://example.org/abc/def/ghi/jkl/mno"));
    }
}
