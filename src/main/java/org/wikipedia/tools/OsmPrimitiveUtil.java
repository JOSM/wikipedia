// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.data.IWikipediaSite;
import org.wikipedia.data.WikipediaSite;

public final class OsmPrimitiveUtil {
    private static final Pattern WIKIPEDIA_PATTERN = Pattern.compile("(.+):(.+)");

    private OsmPrimitiveUtil() {
        // Private constructor to avoid instantiation
    }

    /**
     * Returns the language and article title iff the given primitive has a wikipedia=* tag of
     * the form {@code (.+):(.+)} and the part before the colon is an existent Wikipedia language.
     * @param primitive the primitive for which the Wikipedia site and title of the Wikipedia article will be returned
     * @return A pair of the Wikipedia site as the first component, the article title as second component.
     * Or an empty optional if there is either no wikipedia=* tag, or if the tag value does not match {@code (.+):(.+)},
     * or if the Wikipedia language does not exist or is closed
     */
    public static Optional<Pair<IWikipediaSite, String>> getWikipediaValue(final OsmPrimitive primitive) {
        final String tagValue = primitive.get(OsmTagConstants.Key.WIKIPEDIA);
        if (tagValue != null) {
            final Matcher matcher = WIKIPEDIA_PATTERN.matcher(tagValue);
            if (matcher.matches()) {
                try {
                    final WikipediaSite site = new WikipediaSite(matcher.group(1));
                    if (!site.getSite().isClosed()) {
                        return Optional.of(Pair.create(site, matcher.group(2)));
                    }
                } catch (IOException | IllegalArgumentException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getWikidataValue(final OsmPrimitive primitive) {
        final String wikidataValue = primitive.get(OsmTagConstants.Key.WIKIDATA);
        return Optional.ofNullable(RegexUtil.isValidQId(wikidataValue) ? wikidataValue : null);
    }
}
