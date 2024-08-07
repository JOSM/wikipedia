// License: GPL. For details, see LICENSE file.
package org.wikipedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;
import org.wikipedia.api.wikipedia_action.WikipediaActionApiQuery;
import org.wikipedia.data.WikidataEntry;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.RegexUtil;
import org.wikipedia.tools.XPath;

public final class WikipediaApp {

    /** The base URL for <a href="https://wiki.openstreetmap.org/wiki/WIWOSM">WIWOSM</a> **/
    public static final String WIWOSM_APP = "https://wiwosm.toolforge.org";

    private static final XPath X_PATH = XPath.getInstance();

    private static final String STRING_URI_PIPE = Utils.encodeUrl("|");

    private static final String WIKIDATA = "wikidata";
    private static final String WIKIPEDIA = "wikipedia";

    private final String[] wikipediaKeys;
    private final String wikipediaLang;
    private final SitematrixResult.Sitematrix.Site site;

    private WikipediaApp(final String wikipediaLang) throws IOException {
        this.wikipediaLang = wikipediaLang;
        this.wikipediaKeys = new String[] {WIKIDATA, WIKIPEDIA, WIKIPEDIA + ':' + wikipediaLang};

        final SitematrixResult.Sitematrix sitematrix = ApiQueryClient.query(WikidataActionApiQuery.sitematrix());
        final SitematrixResult.Sitematrix.Language language = sitematrix.getLanguages().stream()
            .filter(it -> wikipediaLang.equalsIgnoreCase(it.getCode())).findFirst().orElse(null);
        if (language != null) {
            this.site = language.getSites().stream().filter(it -> "wiki".equals(it.getCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Wikipedia for language " +  language.getName()
                    + " (" + language.getCode() + ") found!"));
        } else {
            this.site = sitematrix.getSpecialSites().stream().filter(it -> wikipediaLang.equals(it.getCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No wiki site for code '" + wikipediaLang + "' found!"));
        }
    }

    public static WikipediaApp forLanguage(final String wikipediaLang) {
        try {
            return new WikipediaApp(wikipediaLang);
        } catch (IOException | IllegalArgumentException e) {
            Logging.log(Level.WARNING, "Could not initialize Wikipedia app for language '" + wikipediaLang + "'!", e);
            return null;
        }
    }

    static String getMediawikiLocale(Locale locale) {
        if (!locale.getCountry().isEmpty()) {
            return locale.getLanguage() + "-" + locale.getCountry().toLowerCase();
        } else {
            return locale.getLanguage();
        }
    }

    public String getLanguage() {
        return wikipediaLang;
    }

    public String getSiteUrl() {
        return site.getUrl();
    }

    private static HttpClient.Response connect(String url) throws IOException {
        final HttpClient.Response response = HttpClient.create(new URL(url)).setReasonForRequest("Wikipedia").connect();
        if (response.getResponseCode() != 200) {
            throw new IOException("Server responded with HTTP " + response.getResponseCode());
        }
        return response;
    }

    public List<WikipediaEntry> getEntriesFromCoordinates(LatLon min, LatLon max) {
        try {
            // construct url
            final String url = getSiteUrl() + "/w/api.php" +
                "?action=query" +
                "&list=geosearch" +
                "&format=xml" +
                "&gslimit=500" +
                "&gsbbox=" +
                max.lat() + STRING_URI_PIPE + min.lon() +
                STRING_URI_PIPE + min.lat() + STRING_URI_PIPE + max.lon();
            // parse XML document
            try (InputStream in = connect(url).getContent()) {
                final Document doc = newDocumentBuilder().parse(in);
                final String errorInfo = X_PATH.evaluateString("//error/@info", doc);
                if (errorInfo != null && !errorInfo.isEmpty()) {
                    // I18n: {0} is the error message returned by the API
                    new Notification(I18n.tr("Downloading entries with geo coordinates failed: {0}", errorInfo))
                        .setIcon(WikipediaPlugin.NOTIFICATION_ICON)
                        .show();
                }
                final List<WikipediaEntry> entries = X_PATH.evaluateNodes("//gs", doc).stream()
                        .map(node -> {
                            final String name = X_PATH.evaluateString("@title", node);
                            final LatLon latLon = new LatLon(
                                    X_PATH.evaluateDouble("@lat", node),
                                    X_PATH.evaluateDouble("@lon", node));
                            if (WIKIDATA.equals(wikipediaLang)) {
                                return new WikidataEntry(name, null, latLon, null);
                            } else {
                                return new WikipediaEntry(wikipediaLang, name, latLon);
                            }
                        }).collect(Collectors.toList());
                if (WIKIDATA.equals(wikipediaLang)) {
                    return new ArrayList<>(getLabelForWikidata(entries, Locale.getDefault()));
                } else {
                    return entries;
                }
            }
        } catch (Exception ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    public static List<WikidataEntry> getWikidataEntriesForQuery(final String languageForQuery, final String query,
                                                                 final Locale localeForLabels) {
        try {
            final String url = "https://www.wikidata.org/w/api.php" +
                    "?action=wbsearchentities" +
                    "&language=" + languageForQuery +
                    "&strictlanguage=false" +
                    "&search=" + Utils.encodeUrl(query) +
                    "&limit=50" +
                    "&format=xml";
            try (InputStream in = connect(url).getContent()) {
                final Document xml = newDocumentBuilder().parse(in);
                final List<WikidataEntry> r = X_PATH.evaluateNodes("//entity", xml).stream()
                        .map(node -> new WikidataEntry(X_PATH.evaluateString("@id", node)))
                        .collect(Collectors.toList());
                return getLabelForWikidata(r, localeForLabels);
            }
        } catch (Exception ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    public List<WikipediaEntry> getEntriesFromCategory(String category, int depth) {
        try {
            final String url = "https://cats-php.toolforge.org/"
                    + "?lang=" + wikipediaLang
                    + "&depth=" + depth
                    + "&cat=" + Utils.encodeUrl(category);

            try (BufferedReader reader = connect(url).getContentReader()) {
                return reader.lines()
                        .map(line -> new WikipediaEntry(wikipediaLang, line.trim().replace("_", " ")))
                        .collect(Collectors.toList());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<WikipediaEntry> getEntriesFromClipboard(final String wikipediaLang, String clipboardStringContent) {
        if (WIKIDATA.equals(wikipediaLang)) {
            List<WikidataEntry> entries = new ArrayList<>();
            Matcher matcher = RegexUtil.Q_ID_PATTERN.matcher(clipboardStringContent);
            while (matcher.find()) {
                if (RegexUtil.isValidQId(matcher.group())) {
                    entries.add(new WikidataEntry(matcher.group()));
                }
            }
            return new ArrayList<>(getLabelForWikidata(entries, LanguageInfo.getLocale(LanguageInfo.getJOSMLocaleCode())));
        }
        return Pattern.compile("[\\n\\r]+")
                .splitAsStream(clipboardStringContent)
                .map(x -> WikipediaEntry.parseTag("wikipedia:" + wikipediaLang, x))
                .collect(Collectors.toList());
    }

    public void updateWIWOSMStatus(List<WikipediaEntry> entries) {
        if (entries.size() > 20) {
            partitionList(entries, 20).forEach(this::updateWIWOSMStatus);
            return;
        }
        Map<String, Boolean> status = new HashMap<>();
        if (!entries.isEmpty()) {
            final String url = WIWOSM_APP + "/osmjson/getGeoJSON.php?action=check&lang=" + wikipediaLang;
            try {
                final String articles = entries.stream().map(i -> i.article).collect(Collectors.joining(","));
                final String requestBody = "articles=" + Utils.encodeUrl(articles);
                try (BufferedReader reader = HttpClient.create(new URL(url), "POST").setReasonForRequest("Wikipedia")
                                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                                .setRequestBody(requestBody.getBytes(StandardCharsets.UTF_8))
                                .connect().getContentReader()) {
                    reader.lines().forEach(line -> {
                        //[article]\t[0|1]
                        final String[] x = line.split("\t", -1);
                        if (x.length == 2) {
                            status.put(x[0], "1".equals(x[1]));
                        } else {
                            Logging.error("Unknown element " + line);
                        }
                    });
                }
            } catch (Exception ex) {
                throw new JosmRuntimeException(ex);
            }
        }
        for (WikipediaEntry i : entries) {
            i.setWiwosmStatus(status.get(i.article));
        }
    }

    public boolean hasWikipediaTag(final OsmPrimitive p) {
        return p.hasKey(wikipediaKeys);
    }

    /**
     * Check to see if a tagged object has had its wikipedia tag change
     * @param primitive The tagged object to check
     * @param originalKeys The original keys
     * @return {@code true} if the tagged object has had a change in wikipedia keys
     */
    public boolean tagChangeWikipedia(Tagged primitive, Map<String, String> originalKeys) {
        for (String key : wikipediaKeys) {
            // If the key has been added or removed, it has been changed.
            if (primitive.hasKey(key) != originalKeys.containsKey(key) ||
                // If the original key doesn't equal the new key, then it has been changed
                (primitive.hasKey(key) && originalKeys.containsKey(key) && !originalKeys.get(key).equals(primitive.get(key)))) {
                return true;
            }
        }
        return false;
    }

    public Stream<String> getWikipediaArticles(final OsmPrimitive p) {
        if (WIKIDATA.equals(wikipediaLang)) {
            return Stream.of(p.get(WIKIDATA)).filter(Objects::nonNull);
        }
        return Stream
                .of(WIKIPEDIA, WIKIPEDIA + ':' + wikipediaLang)
                .map(key -> WikipediaEntry.parseTag(key, p.get(key)))
                .filter(Objects::nonNull)
                .filter(wp -> wikipediaLang.equals(wp.lang))
                .map(wp -> wp.article);
    }

    /**
     * Returns a map mapping wikipedia articles to wikidata ids.
     * @param articles wikipedia article names
     * @return article / wikidata id map
     */
    public Map<String, String> getWikidataForArticles(Collection<String> articles) {
        final Map<String, String> result = new HashMap<>();
        // maximum of 50 titles
        ListUtil.processInBatches(new ArrayList<>(articles), 50, batch -> result.putAll(resolveWikidataItems(batch)));
        return result;
    }

    /**
     * Get Wikidata IDs. For any unknown IDs, resolve them (normalize and get redirects),
     * and try getting Wikidata IDs again
     * @param articles wikipedia article names
     * @return article / wikidata id map
     */
    private Map<String, String> resolveWikidataItems(Collection<String> articles) {
        final Map<String, String> result = getWikidataForArticles0(articles);
        final List<String> unresolved = articles.stream()
                .filter(title -> !result.containsKey(title))
                .collect(Collectors.toList());
        if (!unresolved.isEmpty()) {
            doResolveWikidataItems(unresolved, result);
        }
        return result;
    }

    private Pair<Map<String, String>, Map<String, String>> doResolveWikidataItems(List<String> unresolved, Map<String, String> result) {
        final Map<String, String> redirects = resolveRedirectsForArticles(unresolved);
        final Map<String, String> result2 = getWikidataForArticles0(redirects.values());
        final List<String> unresolved2 = new ArrayList<>();
        redirects.forEach((original, resolved) -> {
            if (result2.containsKey(resolved)) {
                result.put(original, result2.get(resolved));
            } else if (!Objects.equals(original, resolved)) {
                // Handle double redirection like USA -> United States of America -> United States (as of 9th January 2018)
                unresolved2.add(resolved);
            }
        });
        if (!unresolved2.isEmpty()) {
            Pair<Map<String, String>, Map<String, String>> p = doResolveWikidataItems(unresolved2, result);
            redirects.forEach((original, resolved) -> {
                if (!result2.containsKey(resolved)) {
                    p.a.forEach((original2, resolved2) -> {
                        if (p.b.containsKey(resolved2)) {
                            result.put(original, p.b.get(resolved2));
                        }
                    });
                }
            });
        }
        return Pair.create(redirects, result2);
    }

    private Map<String, String> getWikidataForArticles0(Collection<String> articles) {
        if (articles.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return ApiQueryClient.query(WikidataActionApiQuery.wbgetentities(site, articles))
                .getEntities().values()
                .stream()
                .filter(it -> RegexUtil.isValidQId(it.getId()) && !it.getSitelinks().isEmpty())
                .collect(Collectors.toMap(it -> it.getSitelinks().iterator().next().getTitle(), WbgetentitiesResult.Entity::getId));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Given a list of wikipedia titles, returns a map of corresponding normalized title names,
     * or if the title is a redirect page, the result is the redirect target.
     * @param articles wikipedia articles
     * @return article / wikidata id map
     */
    Map<String, String> resolveRedirectsForArticles(Collection<String> articles) {
        try {
            return articles.stream().collect(Collectors.toMap(it -> it,
                ApiQueryClient.query(WikipediaActionApiQuery.query(site, articles)).getQuery()::resolveRedirect));
        } catch (Exception ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    public List<String> getCategoriesForPrefix(final String prefix) {
        try {
            return ApiQueryClient.query(WikipediaActionApiQuery.categoryPrefixsearch(site, prefix))
                .map(pages ->
                    pages.stream()
                        .map(page -> {
                            final int colonIndex = page.getTitle().indexOf(':') + 1;
                            if (colonIndex > 0 && colonIndex < page.getTitle().length()) {
                                return page.getTitle().substring(colonIndex);
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                ).orElse(new ArrayList<>());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static String getLabelForWikidata(String wikidataId, Locale locale, String... preferredLanguage) {
        try {
            final List<WikidataEntry> entry = Collections.singletonList(new WikidataEntry(wikidataId));
            return getLabelForWikidata(entry, locale, preferredLanguage).get(0).label;
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            Logging.trace(indexOutOfBoundsException);
            return null;
        }
    }

    static List<WikidataEntry> getLabelForWikidata(final List<? extends WikipediaEntry> entries, final Locale locale,
                                                   final String... preferredLanguage) {
        final List<WikidataEntry> wdEntries = entries.stream()
            .map(it -> it instanceof WikidataEntry ? (WikidataEntry) it : null).filter(Objects::nonNull).collect(Collectors.toList());
        if (wdEntries.size() != entries.size()) {
            throw new IllegalArgumentException("The entries given to method `getLabelForWikidata` must all be of type WikidataEntry!");
        }

        final Collection<String> languages = new ArrayList<>();
        if (locale != null) {
            languages.add(getMediawikiLocale(locale));
            languages.add(getMediawikiLocale(new Locale(locale.getLanguage())));
        }
        languages.addAll(Arrays.asList(preferredLanguage));
        languages.add("en");

        final List<WikidataEntry> result = new ArrayList<>(wdEntries.size());
        ListUtil.processInBatches(wdEntries, 50, batch -> {
            try {
                final Map<String, Optional<WbgetentitiesResult.Entity>> entities =
                    ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels(batch.stream().map(it -> it.article)
                        .collect(Collectors.toList())));
                if (entities != null) {
                    for (final WikidataEntry batchEntry : batch) {
                        Optional.ofNullable(entities.get(batchEntry.article)).flatMap(it -> it).ifPresent(entity -> {
                            result.add(new WikidataEntry(
                                batchEntry.article,
                                getFirstLabel(languages, entity.getLabels()),
                                batchEntry.coordinate,
                                getFirstLabel(languages, entity.getDescriptions())
                            ));
                        });
                    }
                }
            } catch (Exception ex) {
                throw new JosmRuntimeException(ex);
            }
        });
        return result;
    }

    private static String getFirstLabel(final Collection<String> languages, final Map<String, String> labelMap) {
        return labelMap.entrySet().stream()
            .filter(it -> languages.contains(it.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(labelMap.values().stream().findFirst().orElse(null)); // fallback to first label
    }

    public Collection<WikipediaEntry> getInterwikiArticles(String article) {
        try {
            final String url = getSiteUrl() + "/w/api.php" +
                    "?action=query" +
                    "&prop=langlinks" +
                    "&titles=" + Utils.encodeUrl(article) +
                    "&lllimit=500" +
                    "&format=xml";
            try (InputStream in = connect(url).getContent()) {
                final Document xml = newDocumentBuilder().parse(in);
                return X_PATH.evaluateNodes("//ll", xml).stream()
                        .map(node -> {
                            final String lang = X_PATH.evaluateString("@lang", node);
                            final String name = node.getTextContent();
                            return new WikipediaEntry(lang, name);
                        }).collect(Collectors.toList());
            }
        } catch (Exception ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    public LatLon getCoordinateForArticle(String article) {
        try {
            final String url = getSiteUrl() + "/w/api.php" +
                    "?action=query" +
                    "&prop=coordinates" +
                    "&titles=" + Utils.encodeUrl(article) +
                    "&format=xml";
            try (InputStream in = connect(url).getContent()) {
                final Document xml = newDocumentBuilder().parse(in);
                final Node node = X_PATH.evaluateNode("//coordinates/co", xml);
                if (node == null) {
                    return null;
                } else {
                    return new LatLon(X_PATH.evaluateDouble("@lat", node), X_PATH.evaluateDouble("@lon", node));
                }
            }
        } catch (Exception ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    public static <T> List<List<T>> partitionList(final List<T> list, final int size) {
        return new AbstractList<List<T>>() {
            @Override
            public List<T> get(int index) {
                final int fromIndex = index * size;
                final int toIndex = Math.min(fromIndex + size, list.size());
                return list.subList(fromIndex, toIndex);
            }

            @Override
            public int size() {
                return (int) Math.ceil(((float) list.size()) / size);
            }
        };
    }

    private static DocumentBuilder newDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Logging.warn("Cannot create DocumentBuilder");
            Logging.warn(e);
            throw new JosmRuntimeException(e);
        }
    }
}
