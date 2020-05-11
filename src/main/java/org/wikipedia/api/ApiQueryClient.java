// License: GPL. For details, see LICENSE file.
package org.wikipedia.api;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.jcs3.engine.behavior.ICacheElement;
import org.openstreetmap.josm.gui.bugreport.BugReportDialog;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.ReportedException;
import org.wikipedia.Caches;

public final class ApiQueryClient {

    private ApiQueryClient() {
        // Private constructor to avoid instantiation
    }

    /**
     * Execute the given query and convert the received JSON to the given Generic class {@code T}.
     * @param query the query that will be executed
     * @param <T> the type to which the JSON is deserialized
     * @return the resulting object received from the API (or from the cache, if the query allows caching, see {@link ApiQuery#getCacheExpiryTime()})
     * @throws IOException if in the process an exception occurs, like a network error, malformed JSON or the like, an {@link IOException}
     *     with localized message is returned
     */
    public static <T> T query(final ApiQuery<T> query) throws IOException {
        final InputStream stream;
        if (query.getCacheExpiryTime() >= 1) {
            // If query should be cached, get the cache element
            final ICacheElement<String, String> cachedElement = Caches.API_RESPONSES.getCacheElement(query.getCacheKey());
            final String cachedValue = cachedElement == null ? null : cachedElement.getVal();
            if (cachedValue == null || System.currentTimeMillis() - cachedElement.getElementAttributes().getCreateTime() > query.getCacheExpiryTime()) {
                // If the cache element is not found or has expired, try to update the value in the cache
                try {
                    final String remoteResponse = new String(IOUtils.toByteArray(getInputStreamForQuery(query)), StandardCharsets.UTF_8);
                    Caches.API_RESPONSES.put(query.getCacheKey(), remoteResponse);
                    Logging.info("Successfully updated API cache for " + query.getCacheKey());
                    return query.deserializeFunc.apply(new ByteArrayInputStream(remoteResponse.getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    if (cachedValue == null) {
                        throw wrapReadDecodeJsonExceptions(e, query.getApiName());
                    }
                    // If there's an expired cache entry, continue using it
                    Logging.log(Level.INFO, "Failed to update the cached API response. Falling back to the cached response.", e);
                }
            }
            Logging.debug("API request is served from cache: {0}", query.getCacheKey());
            stream = new ByteArrayInputStream(cachedValue.getBytes(StandardCharsets.UTF_8));
        } else {
            stream = getInputStreamForQuery(query);
        }

        try {
            return query.deserializeFunc.apply(stream);
        } catch (IOException e) {
            throw wrapReadDecodeJsonExceptions(e, query.getApiName());
        }
    }

    private static InputStream getInputStreamForQuery(final ApiQuery<?> query) throws IOException {
        final HttpClient.Response response;
        try {
            response = query.getHttpClient().connect();
        } catch (IOException e) {
            throw new IOException(I18n.tr(
                // i18n: {0} is the API name, {1} is the name of the exception, {2} is the message of the exception.
                // i18n: Typical values would be: {0}="Wikidata Action API" {1}="UnknownHostException" {2}="www.wikidata.org"
                "Could not connect to the {0}, probably a network issue or the website is currently offline ({1}: {2})",
                query.getApiName(),
                e.getClass().getSimpleName(),
                e.getLocalizedMessage()
            ), e);
        }
        if (response.getResponseCode() != 200) {
            throw new IOException(I18n.tr(
                // i18n: {0} is the API name, {1} is the response code, {2} is the response message.
                // i18n: Typical values would be: {0}="Wikidata Action API" {1}=404 {2}="Not Found"
                "The {0} responded with an unexpected response code: {1} {2}",
                query.getApiName(),
                response.getResponseCode(),
                response.getResponseMessage()
            ));
        }
        final String errorHeader = response.getHeaderField("MediaWiki-API-Error");
        if (errorHeader != null) {
            final IOException wrapperEx = new IOException(I18n.tr(
                // I18n: {0}  is the API name, {1} is the query, normally as URL. {2} is the error message returned from the API
                "The {0} reported an invalid query for {1} ({2}). This is a programming error, please report to the Wikipedia plugin.",
                query.getApiName(),
                query.getCacheKey(),
                errorHeader
            ));
            Logging.error(wrapperEx.getMessage());

            if (!GraphicsEnvironment.isHeadless()) {
                final ReportedException re = BugReport.intercept(wrapperEx).put("component", "Plugin wikipedia").put("keywords", "API " + String.join(" ", query.getTicketKeywords()));
                final BugReportDialog dialog = new BugReportDialog(new BugReport(re));
                dialog.setVisible(true);
            }
            throw wrapperEx;
        }
        return response.getContent();
    }

    /**
     * Wraps the given exception given as parameter into a new {@link IOException}.
     * That new exception has a message already translated via {@link I18n#tr(String, Object...)}.
     * The exception given as parameter is used as cause for the newly created exception.
     * @param exception the exception that should be wrapped
     * @param apiName the name of the API, reading from which caused the exception to occur
     * @return the new exception that wraps the one given as parameter
     */
    private static IOException wrapReadDecodeJsonExceptions(final IOException exception, final String apiName) {
        final IOException wrapper;
        if (exception instanceof JsonParseException || exception instanceof JsonMappingException) {
            // I18n: {0} is the API name
            wrapper = new IOException(I18n.tr("The JSON response from the {0} can''t be decoded!", apiName), exception);
        } else {
            wrapper = new IOException(I18n.tr(
                // i18n: {0} is the name of the API, {1} is the name of the Exception, {2} is the message that exception provides
                "When reading the JSON response from the {0}, an error occured! ({1}: {2})",
                apiName,
                exception.getClass().getSimpleName(),
                exception.getLocalizedMessage()
            ), exception);
        }
        Logging.log(Level.WARNING, wrapper.getMessage(), exception);
        return wrapper;
    }
}
