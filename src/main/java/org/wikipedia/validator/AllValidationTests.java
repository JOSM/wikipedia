// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.I18n;

class AllValidationTests {

    static final ValidationTest<WikidataItemExists> INVALID_QID = new ValidationTest<>(Severity.ERROR, 30_000);
    static final ValidationTest<Test> API_REQUEST_FAILED = new ValidationTest<>(Severity.OTHER, 30_001);
    static final ValidationTest<WikidataItemExists> WIKIDATA_ITEM_DOES_NOT_EXIST = new ValidationTest<>(Severity.ERROR, 30_002);
    static final ValidationTest<WikidataItemExists> WIKIDATA_ITEM_IS_REDIRECT = new ValidationTest<>(Severity.WARNING, 30_003);
    static final ValidationTest<WikipediaAgainstWikidata> WIKIDATA_ITEM_NOT_MATCHING_WIKIPEDIA = new ValidationTest<>(Severity.WARNING, 30_004);
    static final ValidationTest<WikipediaRedirect> WIKIPEDIA_ARTICLE_REDIRECTS = new ValidationTest<>(Severity.WARNING, 30_005);
    static final ValidationTest<WikipediaRedirect> WIKIPEDIA_TAG_INVALID = new ValidationTest<>(Severity.ERROR, 30_006);
    static final ValidationTest<UnusualWikidataClasses> WIKIDATA_TAG_HAS_UNUSUAL_TYPE = new ValidationTest<>(Severity.WARNING, 30_007);
    static final ValidationTest<UnusualWikidataClasses> INVALID_BRAND_WIKIDATA_TAG_FORMAT = new ValidationTest<>(Severity.ERROR, 30_008);

    // i18n: Prefix for the validator messages. Note the space at the end!
    static final String VALIDATOR_MESSAGE_MARKER = I18n.tr("[Wiki] ");

    static final String SEE_OTHER_CATEGORY_VALIDATOR_ERRORS = (
        ValidatorPrefHelper.PREF_OTHER.get()
        ? I18n.tr("See the validator messages of the category ''Other'' for more details.")
        : I18n.tr("Turn on the informational level validator messages in the preferences to see more details.")
    );

    private AllValidationTests() {
        // Private constructor to avoid instantiation
    }

    static class ValidationTest<T extends Test> {
        private final Severity severity;
        private final int code;
        ValidationTest(final Severity severity, final int code) {
            this.severity = severity;
            this.code = code;
        }
        TestError.Builder getBuilder(final T test) {
            return TestError.builder(test, severity, code);
        }
    }
}
