// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.openstreetmap.josm.gui.bugreport.BugReportDialog;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.wikipedia.Caches;
import org.wikipedia.api.ApiQuery;
import org.wikipedia.api.InvalidApiQueryException;
import org.wikipedia.api.wikidata_action.json.SerializationSchema;
import org.wikipedia.tools.CapturingInputStream;

public final class ApiQueryClient {

    private ApiQueryClient() {
        // Private constructor to avoid instantiation
    }

    /**
     * Execute the given query and converts the received JSON to the given Generic class {@code T}.
     * @param query the query that will be executed
     * @param <T> the type to which the JSON is deserialized
     * @return the resulting object received from the API (or from the cache, if the query allows caching, see {@link ApiQuery#getCacheExpiryTime()})
     * @throws IOException if in the process an exception occurs, like a network error, malformed JSON or the like, an {@link IOException}
     *     with localized message is returned
     */
    public static <T> T query(final ApiQuery<T> query) throws IOException {
        if (query.getCacheExpiryTime() >= 1) {
            final ICacheElement<String, String> cachedElement = Caches.API_RESPONSES.getCacheElement(query.getCacheKey());
            final String cachedValue = cachedElement == null ? null : cachedElement.getVal();
            if (cachedValue == null || System.currentTimeMillis() - cachedElement.getElementAttributes().getCreateTime() > query.getCacheExpiryTime()) {
                try {
                    final CapturingInputStream captureStream = new CapturingInputStream(getInputStreamForQuery(query));
                    final T newValue = query.getSchema().getMapper().readValue(captureStream, query.getSchema().getSchemaClass());
                    Caches.API_RESPONSES.put(query.getCacheKey(), new String(captureStream.getCapturedBytes(), StandardCharsets.UTF_8));
                    Logging.info("Successfully updated API cache for " + query.getCacheKey());
                    return newValue;
                } catch (IOException e) {
                    if (cachedValue == null) {
                        throw new IOException(I18n.tr("Failed to read from the API and there's no response available in the cache to use instead!"), e);
                    }
                    Logging.log(Level.INFO, "Failed to update the cached API response. Falling back to the cached response.", e);
                }
            }
            Logging.info("API request is served from cache: {0}", query.getCacheKey());
            return decodeJson(query.getSchema(), new ByteArrayInputStream(cachedElement.getVal().getBytes(StandardCharsets.UTF_8)));
        }
        return decodeJson(query.getSchema(), getInputStreamForQuery(query));
    }

    private static InputStream getInputStreamForQuery(final ApiQuery query) throws IOException {
        final HttpClient.Response response;
        try {
            response = query.getHttpClient().connect();
        } catch (IOException e) {
            throw new IOException(I18n.tr(
                // i18n: {0} is the name of the exception, {1} is the message of the exception. Typical values would be: {0}="UnknownHostException" {1}="www.wikidata.org"
                "Could not connect to the Wikidata Action API, probably a network issue or the website is currently offline ({0}: {1})",
                e.getClass().getSimpleName(),
                e.getLocalizedMessage()
            ), e);
        }
        if (response.getResponseCode() != 200) {
            throw new IOException(I18n.tr(
                // i18n: {0} is the response code, {1} is the response message. Typical values would be: {0}=404 {1}="Not Found"
                "The Wikidata Action API responded with an unexpected response code: {0} {1}",
                response.getResponseCode(),
                response.getResponseMessage()
            ));
        }
        final String errorHeader = response.getHeaderField("MediaWiki-API-Error");
        if (errorHeader != null) {
            final IOException wrapperEx = new IOException(I18n.tr(
                // I18n: {0} is the query, normally as URL. {1} is the error message returned from the API
                "The Wikidata Action API reported an invalid query for {0} ({1}). This is a programming error, please report to the Wikipedia plugin.",
                query,
                errorHeader
            ));
            Logging.error(wrapperEx.getMessage());

            if (!GraphicsEnvironment.isHeadless()) {
                BugReport report = new BugReport(BugReport.intercept(new InvalidApiQueryException(query.getUrl())));
                BugReportDialog dialog = new BugReportDialog(report);
                dialog.setVisible(true);
            }
            throw wrapperEx;
        }
        return response.getContent();
    }

    private static <T> T decodeJson(final SerializationSchema<T> schema, final InputStream stream) throws IOException {
        try {
            return schema.getMapper().readValue(stream, schema.getSchemaClass());
        } catch (IOException e) {
            final IOException wrapper;
            if (e instanceof JsonParseException || e instanceof JsonMappingException) {
                wrapper = new IOException(I18n.tr("The cached JSON response from the Wikidata Action API can't be decoded!"), e);
            } else {
                wrapper = new IOException(I18n.tr(
                    "When reading the JSON response from the Wikidata Action API, an error occured! ({0}: {1})",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
                ), e);
            }
            Logging.log(Level.WARNING, wrapper.getMessage(), e);
            throw wrapper;
        }
    }
}
