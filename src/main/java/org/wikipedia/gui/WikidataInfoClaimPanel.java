package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.Collection;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetclaimsResult;

class WikidataInfoClaimPanel extends ProgressJPanel {

    private final JPanel mainPanel;
    private String qIdBeingDownloaded;

    WikidataInfoClaimPanel() {
        super();
        this.mainPanel = new JPanel();
        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
    }

    void downloadStatementsFor(final String qId) {
        this.qIdBeingDownloaded = qId;
        new Thread(() -> {
            try {
                mainPanel.removeAll();
                showProgress(I18n.tr("Download statements for {0}…", qId));
                final Collection<WbgetclaimsResult.Claim> result = ApiQueryClient.query(WikidataActionApiQuery.wbgetclaims(qId));
                if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                    synchronized (mainPanel) {
                        mainPanel.removeAll();
                        mainPanel.setLayout(new GridLayout(result.size(), 1));
                        result.forEach(claim -> {
                            final WbgetclaimsResult.Claim.MainSnak.DataValue value = claim.getMainSnak().getDataValue(); // nullable
                            mainPanel.add(new StatementPanel(claim.getMainSnak().getProperty(), value == null ? I18n.tr("Unknown datatype!") : value.toString()));
                        });
                        hideProgress();
                    }
                }
            } catch (IOException e) {
                new Notification(I18n.tr("Failed to download statements for Wikidata item {0}!", qId))
                    .setIcon(WikipediaPlugin.W_IMAGE.get())
                    .show();
            }
            hideProgress();
        }).start();

    }
}
