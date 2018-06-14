// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import org.wikipedia.api.wikidata_action.json.SitematrixResult;

public interface IWikipediaSite {

    /**
     * @return the site for a certain language, as returned by {@code action=sitematrix} with the Wikidata Action API
     */
    public SitematrixResult.Sitematrix.Site getSite();

    /**
     * @return the language code of the wikipedia, always non-null
     */
    public String getLanguageCode();
}
