// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;

public class CheckEntityExistsResultTest {
    @Test
    public void test() throws IOException {
        final CheckEntityExistsResult enitityQueryResult = SerializationSchema.WBGETENTITIES.getMapper().readValue(
            CheckEntityExistsResultTest.class.getResourceAsStream("response-wbgetentities-checkExists-Q1_Q1234567.json"),
            SerializationSchema.WBGETENTITIES.getSchemaClass()
        );
        assertEquals(1, enitityQueryResult.getSuccess());
        assertEquals(1, enitityQueryResult.getEntities().size());
        assertEquals(1, enitityQueryResult.getMissingEntities().size());
        assertEquals("Q1", enitityQueryResult.getEntities().get("Q1").getId());
        assertEquals("item", enitityQueryResult.getEntities().get("Q1").getType());
        assertEquals("Q1234567", enitityQueryResult.getMissingEntities().iterator().next().getId());
    }
}
