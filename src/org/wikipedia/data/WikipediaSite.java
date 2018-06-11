// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;

/**
 * A Wikipedia site for a certain language. For each instance of this class you can assume that there is a Wikipedia in this language.
 */
public class WikipediaSite {
    private final SitematrixResult.Sitematrix.Site site;

    /**
     * Constructs a Wikipedia site for a given language (iff such a Wikipedia exists).
     * @param langCode the language code of the wiki
     * @throws IOException if the sitematrix can't be read, see {@link ApiQueryClient#query(ApiQuery)}
     * @throws IllegalArgumentException if there is no Wikipedia for the given language
     */
    public WikipediaSite(final String langCode) throws IOException, IllegalArgumentException {
        Objects.requireNonNull(langCode);
        final SitematrixResult sitematrix = ApiQueryClient.query(WikidataActionApiQuery.sitematrix());
        final Optional<SitematrixResult.Sitematrix.Language> language = sitematrix.getSitematrix().getLanguages().stream()
            .filter(it -> langCode.equals(it.getCode()))
            .findFirst();
        this.site = language
            .orElseThrow(() -> new IllegalArgumentException(I18n.tr("{0} is an illegal language code!", langCode)))
            .getSites().stream()
                .filter(it -> "wiki".equals(it.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    I18n.tr("For language {0} there is no Wikipedia site!", language.get().getCode())
                ));
    }

    /**
     * @return the site for a certain language, as returned by {@code action=sitematrix} with the Wikidata Action API
     */
    public SitematrixResult.Sitematrix.Site getSite() {
        return site;
    }
}
