// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class CheckEntityExistsResultTest {
    @Test
    public void test() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        final CheckEntityExistsResult enitityQueryResult = mapper.readValue(
            CheckEntityExistsResultTest.class.getResourceAsStream("response-wbgetentities-checkExists-Q1_Q1234567.json"),
            CheckEntityExistsResult.class
        );
        assertEquals(1, enitityQueryResult.getSuccess());
        assertEquals(2, enitityQueryResult.getEntities().size());
        assertEquals("Q1", enitityQueryResult.getEntities().get("Q1").getId());
        assertEquals("item", enitityQueryResult.getEntities().get("Q1").getType());
        assertEquals("Q1234567", enitityQueryResult.getEntities().get("Q1234567").getId());
        assertNull(enitityQueryResult.getEntities().get("Q1234567").getType());

    }
}
