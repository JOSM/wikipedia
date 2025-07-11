package org.wikipedia.validator;

import static org.wikipedia.validator.AllValidationTests.SEE_OTHER_CATEGORY_VALIDATOR_ERRORS;
import static org.wikipedia.validator.AllValidationTests.VALIDATOR_MESSAGE_MARKER;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
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
    private static final Pattern CONTAINS_URL_PATTERN = Pattern.compile(".*(https?://|wikipedia\\.org/|wikimedia.org/).*");
    private static final Pattern WIKIPEDIA_TAG_VALUE_PATTERN = Pattern.compile("([a-zA-Z\\-]+):(.+)");

    private final SitematrixResult.Sitematrix sitematrix;

    private static final Notification NETWORK_FAILED_NOTIFICATION = new Notification(
        I18n.tr("Could not get all possible wikipedia sites over the internet.") + "\n" + SEE_OTHER_CATEGORY_VALIDATOR_ERRORS
    ).setIcon(WikipediaPlugin.LOGO);

    public WikipediaValueFormat() {
        super(
            VALIDATOR_MESSAGE_MARKER + I18n.tr("Check format of wikipedia=* tags and subtags"),
            I18n.tr("Just checks the format of the Wikipedia tag (no validation if articles exist), with autofixes for when tag value is a URL")
        );

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
        Optional.of(OsmTagConstants.Key.WIKIPEDIA).flatMap(k -> Optional.ofNullable(p.get(k)).map(v -> new Tag(k, v))).flatMap(tag ->
            getFullUrlCheckError(p, tag.getKey(), tag.getValue()).or(
                () -> getLanguageArticleFormatError(p, tag.getKey(), tag.getValue())
            )
            // checkUrlDecode() is tested by core
        ).ifPresent(it -> errors.add(it.build()));
        p.keySet().stream().filter(Objects::nonNull).filter(it -> it.endsWith(":wikipedia")).collect(Collectors.toMap(it -> it, p::get)).forEach((key, value) -> {
            getFullUrlCheckError(p, key, value).or(
                () -> getLanguageArticleFormatError(p, key, value)
            ).ifPresent(it -> errors.add(it.build()));
            checkUrlDecode(p, key, value);
        });
        p.keySet().stream().filter(Objects::nonNull).filter(it -> it.startsWith("wikipedia:")).collect(Collectors.toMap(it -> it, p::get)).forEach((key, value) -> {
            getFullUrlCheckError(p, key, value, I18n.tr("Use only the article title instead of a URL for wikipedia:‹language› tags"), entry ->
                Optional.ofNullable(key.equals("wikipedia:" + entry.lang) ? new Tag(key, entry.article) : null) // needs manual attention if URL does not match the language in the key
            ).ifPresent(it -> errors.add(it.build()));
            checkUrlDecode(p, key, value);
        });
    }

    private Optional<TestError.Builder> getLanguageArticleFormatError(final OsmPrimitive p, final String key, final String value) {
        final Matcher matcher = WIKIPEDIA_TAG_VALUE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.of(
                AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                    .message(
                        AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("Wikipedia tag has invalid format!"),
                        I18n.marktr("The value ''{0}'' is not allowed for the wikipedia=* tag"),
                        value
                    )
                    .primitives(p)
            );
        } else if (!matcher.group(1).equals(matcher.group(1).toLowerCase(Locale.ENGLISH))) {
            return Optional.of(
                AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                    .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("The language prefix of wikipedia tags should be lowercase!"))
                    .fix(() -> new ChangePropertyCommand(p, key, value.equals(p.get(key)) ? matcher.group(1).toLowerCase(Locale.ENGLISH) + ':' + matcher.group(2) : p.get(key)))
                    .primitives(p)
            );
        } else if (sitematrix != null) {
            final Optional<SitematrixResult.Sitematrix.Site> site = sitematrix.getLanguages().stream().filter(it -> Objects.equals(it.getCode(), matcher.group(1))).findFirst().flatMap(it -> it.getSites().stream().filter(s -> "wiki".equals(s.getCode())).findFirst());
            if (!site.isPresent()) {
                return Optional.of(
                    AllValidationTests.WIKIPEDIA_TAG_INVALID.getBuilder(this)
                        .message(VALIDATOR_MESSAGE_MARKER + I18n.tr("Unknown Wikipedia language prefix ''{0}''!", matcher.group(1)))
                        .primitives(p)
                );
            }
        }
        return Optional.empty();
    }

    private void checkUrlDecode(final OsmPrimitive p, final String key, final String value) {
        final String decodedValue = Utils.decodeUrl(value).replace('_', ' ');
        if (!decodedValue.equals(value)) {
            errors.add(
                AllValidationTests.WIKIPEDIA_TAG_URL_ENCODED.getBuilder(this)
                    .primitives(p)
                    .fix(() -> new ChangePropertyCommand(p, key, Utils.decodeUrl(p.get(key)).replace('_', ' ')))
                    .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("{0}=* tag should not have URL-encoded values.", key))
                    .build()
            );
        }
    }

    private Optional<TestError.Builder> getFullUrlCheckError(final OsmPrimitive p, final String key, final String value) {
        return getFullUrlCheckError(p, key, value, I18n.tr("The value of a wikipedia tag should be given as ‹language›:‹Article title› instead of the full URL"), entry -> Optional.of(new Tag(key, entry.toOsmTagValue())));
    }

    private Optional<TestError.Builder> getFullUrlCheckError(final OsmPrimitive p, final String key, final String value, final String message, final Function<WikipediaEntry, Optional<Tag>> newTagGetter) {
        return WikipediaEntry.fromUrl(value).map(newTagGetter).flatMap(newTag ->
                Optional.of(
                    AllValidationTests.WIKIPEDIA_TAG_VALUE_IS_FULL_URL.getBuilder(this)
                        .primitives(p)
                        .fix(newTag.map(it -> ((Supplier<Command>) () -> new ChangePropertyCommand(p, it.getKey(), it.getValue()))).orElse(null))
                        .message(
                            AllValidationTests.VALIDATOR_MESSAGE_MARKER + message,
                            I18n.marktr("Change {0}={1} to {2}"),
                            key,
                            value,
                            newTag.map(it -> it.getKey() + "=" + it.getValue()).orElse(I18n.tr("a new value (manual intervention needed due to language conflict)"))
                        )
                )
            ).or(
            () -> Optional.ofNullable(
                CONTAINS_URL_PATTERN.matcher(value).matches()
                    ? AllValidationTests.WIKIPEDIA_TAG_VALUE_CONTAINS_URL.getBuilder(this)
                        .primitives(p)
                        .message(AllValidationTests.VALIDATOR_MESSAGE_MARKER + I18n.tr("It looks like a wikipedia tag contains a URL. Use the format ''‹language›:‹article title› instead!''"), "{0}={1}", key, value)
                    : null
            )
        );
    }
}
