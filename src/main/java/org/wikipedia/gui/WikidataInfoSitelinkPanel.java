package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.SitematrixResult;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;

public class WikidataInfoSitelinkPanel extends ProgressJPanel {

    private final JPanel mainPanel;
    private String qIdBeingDownloaded;

    WikidataInfoSitelinkPanel() {
        this.mainPanel = new JPanel();
        this.mainPanel.setLayout(new GridBagLayout());
        add(new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    void downloadSitelinksFor(final String qId) {
        this.qIdBeingDownloaded = qId;
        new Thread(() -> {
            try {
                mainPanel.removeAll();
                showProgress(I18n.tr("Download sitelinks for {0}…", qId));
                final Collection<WbgetentitiesResult.Entity.Sitelink> sitelinks =
                    ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesSitelinks(qId));
                final SitematrixResult.Sitematrix sitematrix = ApiQueryClient.query(WikidataActionApiQuery.sitematrix());
                if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                    synchronized (mainPanel) {
                        mainPanel.removeAll();
                        final GBC gbc = GBC.std().fill().grid(0, 0);
                        sitelinks.stream()
                            .sorted(Comparator.comparing(WbgetentitiesResult.Entity.Sitelink::getSite))
                            .forEach(sitelink -> {
                                final Optional<SitematrixResult.Sitematrix.Site> site = sitematrix.getSiteForDbname(sitelink.getSite());
                                if (!site.isPresent()) {
                                    Logging.warn("Could not find site {0} for sitelink {1}!", sitelink.getSite(), sitelink.getTitle());
                                }
                                site.ifPresent(s -> {
                                    final JButton linkButton = new JButton();
                                    linkButton.setAction(new AbstractAction() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            final String uri = s.getUrl() + "/w/index.php?title=" + Utils.encodeUrl(sitelink.getTitle());
                                            final String error = OpenBrowser.displayUrl(uri);
                                            if (error != null) {
                                                new Notification(I18n.tr("Can't open website {0} in browser! Error message: {1}", uri, error))
                                                    .setIcon(WikipediaPlugin.W_IMAGE.get())
                                                    .show();
                                            }
                                        }
                                    });
                                    linkButton.setText(
                                        s.getDbName()  + ": " + s.getSiteName() + (s.getLanguage() == null ? "" : " " + s.getLanguage().getName())
                                    );
                                    mainPanel.add(linkButton, gbc);
                                    gbc.gridy++;
                                });
                            });
                        hideProgress();
                    }
                }
            } catch (IOException e) {
                new Notification(I18n.tr("Failed to download sitelinks for Wikidata item {0}!", qId))
                    .setIcon(WikipediaPlugin.W_IMAGE.get())
                    .show();
            }
            hideProgress();
        }).start();
    }

}
