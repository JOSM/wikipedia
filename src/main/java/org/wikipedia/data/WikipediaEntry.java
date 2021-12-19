// License: GPL. For details, see LICENSE file.
package org.wikipedia.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.tools.FunctionalUtil;

public class WikipediaEntry implements Comparable<WikipediaEntry> {

    private static final Pattern WIKIPEDIA_FULL_URL_PATTERN = Pattern.compile("((https?:)?//)?([a-z-]+)(\\.m)?\\.wikipedia\\.org/wiki/(.+)");
    private static final Pattern WIKIPEDIA_ALTERNATIVE_URL_PATTERN = Pattern.compile("((https?:)?//)?([a-z-]+)(\\.m)?\\.wikipedia\\.org/w/index.php\\?(.+)");

    public final String lang;
    public final String article;
    public final LatLon coordinate;
    private Boolean wiwosmStatus;

    public WikipediaEntry(String lang, String article) {
        this(lang, article, null);
    }

    public WikipediaEntry(String lang, String article, LatLon coordinate) {
        this.lang = lang;
        this.article = article;
        this.coordinate = coordinate;
    }

    public static Optional<WikipediaEntry> fromUrl(final String value) {
        return FunctionalUtil.or(
            Optional.ofNullable(value)
                .map(WIKIPEDIA_FULL_URL_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(it -> new WikipediaEntry(it.group(3), Utils.decodeUrl(it.group(5)).replace('_', ' '))),
            () -> Optional.ofNullable(value)
                .map(WIKIPEDIA_ALTERNATIVE_URL_PATTERN::matcher)
                .filter(Matcher::matches)
                .flatMap(it -> {
                    final String titlePrefix = "title=";
                    return Arrays.stream(it.group(5).split("&"))
                        .filter(param -> param != null && param.startsWith(titlePrefix))
                        .map(param -> param.substring(titlePrefix.length()))
                        .map(title -> new WikipediaEntry(it.group(3), Utils.decodeUrl(title).replace('_', ' ')))
                        .findFirst();
                })
        );
    }

    public static WikipediaEntry parseTag(String key, String value) {
        final Optional<WikipediaEntry> fromUrl = fromUrl(value);
        if (value == null) {
            return null;
        } else if (fromUrl.isPresent()) {
            //wikipedia=http...
            return fromUrl.get();
        } else if (value.contains(":")) {
            //wikipedia=[lang]:[article]
            //wikipedia:[lang]=[lang]:[article]
            final String[] item = Utils.decodeUrl(value).split(":", 2);
            final String article = item[1].replace("_", " ");
            return new WikipediaEntry(item[0], article);
        } else if (key.startsWith("wikipedia:")) {
            //wikipedia:[lang]=[lang]:[article]
            //wikipedia:[lang]=[article]
            final String lang = key.split(":", 2)[1];
            final String[] item = Utils.decodeUrl(value).split(":", 2);
            final String article = item[item.length == 2 ? 1 : 0].replace("_", " ");
            return new WikipediaEntry(lang, article);
        } else {
            return null;
        }
    }

    public Tag createWikipediaTag() {
        return new Tag("wikipedia", toOsmTagValue());
    }

    public void setWiwosmStatus(Boolean wiwosmStatus) {
        this.wiwosmStatus = wiwosmStatus;
    }

    public Boolean getWiwosmStatus() {
        return wiwosmStatus;
    }

    public Optional<String> getBrowserUrl() {
        return Optional.ofNullable(WikipediaApp.forLanguage(lang)).map(it -> it.getSiteUrl() + "/wiki/" + Utils.encodeUrl(article.replace(' ', '_')));
    }

    public String getLabelText() {
        return article;
    }

    public String getSearchText() {
        return article;
    }

    public String toOsmTagValue() {
        return lang + ':' + article;
    }

    @Override
    public String toString() {
        return article;
    }

    @Override
    public int compareTo(WikipediaEntry o) {
        return AlphanumComparator.getInstance().compare(article, o.article);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WikipediaEntry)) return false;
        final WikipediaEntry that = (WikipediaEntry) o;
        return Objects.equals(lang, that.lang) &&
                Objects.equals(article, that.article);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lang, article);
    }
}
