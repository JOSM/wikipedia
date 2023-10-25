package org.wikipedia.api.wikidata_action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.json.WbgetclaimsResult;
import org.wikipedia.testutils.ResourceFileLoader;

class WbgetentitiesClaimsTest extends WikidataActionApiTestAbstract {

    @Test
    void test() throws IOException, URISyntaxException {
        simpleJsonStub(ResourceFileLoader.getResourceBytes(WikidataActionApiQueryTest.class, "response/wbgetentities/claims_Q2.json"));

        final Optional<Collection<WbgetclaimsResult.Claim>> result = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesClaims("Q2"));

        assertTrue(result.isPresent());
        assertEquals(252, result.get().size()); // Total number of claims
        assertEquals(WbgetclaimsResult.Claim.RANK.PREFERRED, result.get().stream().filter(it -> "Q2$9bbb058a-48ab-0a81-e88e-82bba6b2abe5".equals(it.getId())).findFirst().get().getRank());
        assertEquals("P31", result.get().stream().filter(it -> "Q2$50fad68d-4f91-f878-6f29-e655af54690e".equals(it.getId())).findFirst().get().getMainSnak().getProperty());

        final Map<String, List<WbgetclaimsResult.Claim>> claimMap = result.get().stream().collect(Collectors.groupingBy(it -> it.getMainSnak().getProperty()));
        assertEquals(119, claimMap.keySet().size()); // Number of different properties for which claims exist

        simpleRequestVerify("action=wbgetentities&format=json&formatversion=2&ids=Q2&props=claims&utf8=1");
    }
}
