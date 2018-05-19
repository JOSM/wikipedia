// License: GPL. For details, see LICENSE file.
package org.wikipedia.api.wikidata_action;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstreetmap.josm.gui.bugreport.BugReportDialog;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.wikipedia.api.InvalidApiQueryException;

public final class ApiQueryClient {
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    static {
        JSON_OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private ApiQueryClient() {
        // Private constructor to avoid instantiation
    }

    /**
     * Queries the given URL and converts the received JSON to the given class using the Jackson library
     * @param url the {@link URL} to query
     * @param klass the class object of the desired type
     * @param <T> the type to which the JSON is deserialized
     * @return the deserialized object
     * @throws IOException if any error occurs while executing the query, with a translated message that can be shown to the user.
     */
    public static <T> T query(final URL url, final Class<T> klass) throws IOException {
        final HttpClient.Response response;
        try {
            response = HttpClient.create(url)
                .setAccept("application/json")
                .setHeader("User-Agent", "JOSM-wikipedia (). Report issues at https://josm.openstreetmap.de/newticket?component=Plugin%20wikipedia&priority=major&keywords=api%20wikidata%20ActionAPI")
                .connect();
        } catch (IOException e) {
            // i18n: {0} is the name of the exception, {1} is the message of the exception. Typical values would be: {0}="UnknownHostException" {1}="www.wikidata.org"
            throw new IOException(I18n.tr("Could not connect to the Wikidata Action API, probably a network issue or the website is currently offline ({0}: {1})", e.getClass().getSimpleName(), e.getLocalizedMessage()), e);
        }
        if (response.getResponseCode() != 200) {
            // i18n: {0} is the response code, {1} is the response message. Typical values would be: {0}=404 {1}="Not Found"
            throw new IOException(I18n.tr("The Wikidata Action API responded with an unexpected response code: {0} {1}", response.getResponseCode(), response.getResponseMessage()));
        }
        final String errorHeader = response.getHeaderField("MediaWiki-API-Error");
        if (errorHeader != null) {
            Logging.error(I18n.tr("The Wikidata Action API reported a query failure for URL {0} ({1}). This is a programming error, please report to the Wikipedia plugin.", url, errorHeader));

            BugReport report = new BugReport(BugReport.intercept(new InvalidApiQueryException(url)));
            BugReportDialog dialog = new BugReportDialog(report);
            dialog.setVisible(true);
            throw new IOException(I18n.tr("The Wikidata Action API reported that the query was invalid! Please report as bug to the Wikipedia plugin!"));
        }
        try {

            return JSON_OBJECT_MAPPER.readValue(response.getContent(), klass);
        } catch (JsonMappingException | JsonParseException e) {
            throw new IOException(I18n.tr("The JSON response from the Wikidata Action API can't be read!"), e);
        } catch (IOException e) {
            throw new IOException(I18n.tr("When reading the JSON response from the Wikidata Action API, an error occured! ({0}: {1})", e.getClass().getSimpleName(), e.getLocalizedMessage()), e);
        }
    }
}
