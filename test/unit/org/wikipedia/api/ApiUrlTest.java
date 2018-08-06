// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class ApiUrlTest {

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedUrl() {
        ApiUrl.url("malformedURL");
    }

    @Test
    public void testUrl() throws MalformedURLException {
        assertEquals(new URL("https://example.org"), ApiUrl.url("https://example.org"));
        assertEquals(new URL("https://example.org/abc"), ApiUrl.url("https://example.org/abc"));
        assertEquals(new URL("https://example.org/abc/def/ghi/jkl/mno"), ApiUrl.url("https://example.org/abc/def/ghi/jkl/mno"));
    }
}
