// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import java.io.IOException;
import java.util.Objects;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;

/**
 * A Wikipedia site for a certain language. For each instance of this class you can assume that there is a Wikipedia in this language.
 */
public class WikipediaSite implements IWikipediaSite {
    private final SitematrixResult.Sitematrix.Site site;
    private final SitematrixResult.Sitematrix.Language language;

    /**
     * Constructs a Wikipedia site for a given language (iff such a Wikipedia exists).
     * @param langCode the language code of the wiki
     * @throws IOException if the sitematrix can't be read, see {@link ApiQueryClient#query(ApiQuery)}
     * @throws IllegalArgumentException if there is no Wikipedia for the given language
     */
    public WikipediaSite(final String langCode) throws IOException, IllegalArgumentException {
        Objects.requireNonNull(langCode);
        final SitematrixResult.Sitematrix sitematrix = ApiQueryClient.query(WikidataActionApiQuery.sitematrix());
        language = sitematrix.getLanguages().stream()
            .filter(it -> langCode.equals(it.getCode()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(I18n.tr("''{0}'' is an illegal language code!", langCode)));
        this.site = language
            .getSites().stream()
                .filter(it -> "wiki".equals(it.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    I18n.tr("There is no Wikipedia site for language ''{0}''!", language.getCode())
                ));
    }

    @Override
    public SitematrixResult.Sitematrix.Site getSite() {
        return site;
    }

    @Override
    public String getLanguageCode() {
        return language.getCode();
    }
}
