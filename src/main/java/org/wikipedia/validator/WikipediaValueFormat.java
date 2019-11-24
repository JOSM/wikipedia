package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.OsmTagConstants;

public class WikipediaValueFormat extends Test.TagTest {
    private static final Pattern WIKIPEDIA_TAG_VALUE_PATTERN = Pattern.compile("([a-zA-Z\\-]+):(.+)");

    private final SitematrixResult.Sitematrix sitematrix;

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not get all possible wikipedia sites over the internet.") + "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    public WikipediaValueFormat() {
        super("Check format of wikipedia=* tag");

        SitematrixResult.Sitematrix sitematrix;
        try {
            sitematrix = ApiQueryClient.query(WikidataActionApiQuery.sitematrix());
        } catch (IOException e) {
            sitematrix = null;
            NETWORK_FAILED_NOTIFICATION.show();
            errors.add(AllValidationTests.API_REQUEST_FAILED.getBuilder(this).message(e.getMessage()).build());
        }
        this.sitematrix = sitematrix;
    }

    @Override
    public void check(OsmPrimitive p) {
        Optional.of(OsmTagConstants.Key.WIKIPEDIA).flatMap(k -> Optional.ofNullable(p.get(k)).map(v -> new Tag(k, v))).ifPresent(tag -> {
            checkFullUrl(p, tag.getKey(), tag.getValue());
            checkLanguageArticleFormat(p, tag.getKey(), tag.getValue());
            // checkUrlDecode() is tested by core
        });
        p.keySet().stream().filter(Objects::nonNull).filter(it -> it.endsWith(":wikipedia")).collect(Collectors.toMap(it -> it, p::get)).forEach((key, value) -> {
            checkFullUrl(p, key, value);
            checkUrlDecode(p, key, value);
            checkLanguageArticleFormat(p, key, value);
        });
        p.keySet().stream().filter(Objects::nonNull).filter(it -> it.startsWith("wikipedia:")).collect(Collectors.toMap(it -> it, p::get)).forEach((key, value) -> {
            checkFullUrl(p, key, value, I18n.tr("Use only the article title instead of a URL for wikipedia:‹language› tags"), entry -> {
                if (key.equals("wikipedia:" + entry.lang)) {
                    return new Tag(key, entry.article);
                }
                return null; // needs manual attention if URL does not match the language in the key
            });
            // checkUrlDecode() is tested by core
        });
    }


    private void checkLanguageArticleFormat(final OsmPrimitive p, final String key, final String value) {
        final Matcher matcher = WIKIPEDIA_TAG_VALUE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                    .message(
                        AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikipedia tag has invalid format!"),
                        I18n.marktr("The value ''{0}'' is not allowed for the wikipedia=* tag"),
                        value
                    )
                    .primitives(p)
                    .build()
            );
        } else if (!matcher.group(1).equals(matcher.group(1).toLowerCase(Locale.ENGLISH))) {
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                    .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("The language prefix of wikipedia tags should be lowercase!"))
                    .fix(() -> new ChangePropertyCommand(p, key, value.equals(p.get(key)) ? matcher.group(1).toLowerCase(Locale.ENGLISH) + ':' + matcher.group(2) : p.get(key)))
                    .primitives(p)
                    .build()
            );
        } else if (sitematrix != null) {
            final Optional<SitematrixResult.Sitematrix.Site> site = sitematrix.getLanguages().stream().filter(it -> Objects.equals(it.getCode(), matcher.group(1))).findFirst().flatMap(it -> it.getSites().stream().filter(s -> "wiki".equals(s.getCode())).findFirst());
            if (!site.isPresent()) {
                errors.add(
                    AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                        .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("Unknown Wikipedia language prefix ''{0}''!", matcher.group(1)))
                        .primitives(p)
                        .build()
                );
            }
        }
    }

    private void checkUrlDecode(final OsmPrimitive p, final String key, final String value) {
        final String decodedValue = Utils.decodeUrl(value).replace('_', ' ');
        if (!decodedValue.equals(value)) {
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_VALUE_IS_FULL_URL.getBuilder(this)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, Utils.decodeUrl(p.get(key)).replace('_', ' ')))
                    .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("{0}=* tag should not have URL-encoded values.", key))
                    .build()
            );
        }
    }

    private void checkFullUrl(final OsmPrimitive p, final String key, final String value) {
        checkFullUrl(p, key, value, I18n.tr("The value of a wikipedia tag should be given as ‹language›:‹Article lemma› instead of the full URL"), entry -> new Tag(key, entry.toOsmTagValue()));
    }

    private void checkFullUrl(final OsmPrimitive p, final String key, final String value, final String message, final Function<WikipediaEntry, Tag> newTagGetter) {
        WikipediaEntry.fromUrl(value).ifPresent(entry -> {
            final Tag newTag = newTagGetter.apply(entry);
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_VALUE_IS_FULL_URL.getBuilder(this)
                    .primitives(p)
                    .fix(newTag == null ? null : () -> new ChangePropertyCommand(p, newTag.getKey(), newTag.getValue()))
                    .message(
                        AllValidationTests.VALIDATOR_MESSAGE_MARKER + message,
                        I18n.marktr("Change {0}={1} to {2}"),
                        key,
                        value,
                        newTag == null ? I18n.tr("a new value (manual intervention needed due to language conflict)") : newTag.getKey() + "=" + newTag.getValue()
                    )
                    .build()
            );
        });
    }
}
