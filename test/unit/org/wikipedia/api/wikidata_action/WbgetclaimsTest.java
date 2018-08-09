package org.wikipedia.api.wikidata_action;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.WbgetclaimsResult;
import org.wikipedia.testutils.ResourceFileLoader;

public class WbgetclaimsTest extends WikidataActionApiTestAbstract {

    @Test
    public void test() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetclaims/Q2.json"));

        final Collection<WbgetclaimsResult.Claim> result = ApiQueryClient.query(WikidataActionApiQuery.wbgetclaims("Q2"));

        assertEquals(216, result.size()); // Total number of claims
        assertEquals(WbgetclaimsResult.Claim.RANK.PREFERRED, result.stream().filter(it -> "Q2$9bbb058a-48ab-0a81-e88e-82bba6b2abe5".equals(it.getId())).findFirst().get().getRank());
        assertEquals("P31", result.stream().filter(it -> "Q2$50fad68d-4f91-f878-6f29-e655af54690e".equals(it.getId())).findFirst().get().getMainSnak().getProperty());

        final Map<String, List<WbgetclaimsResult.Claim>> claimMap = result.stream().collect(Collectors.groupingBy(it -> it.getMainSnak().getProperty()));
        assertEquals(97, claimMap.keySet().size()); // Number of different properties for which claims exist

        simpleRequestVerify("format=json&utf8=1&formatversion=1&action=wbgetclaims&props=&entity=Q2");
    }
}
