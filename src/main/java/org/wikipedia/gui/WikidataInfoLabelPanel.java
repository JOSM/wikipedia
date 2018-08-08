package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;

/**
 * Panel displaying the labels for a Wikidata item
 */
class WikidataInfoLabelPanel extends JPanel {

    private static final JProgressBar downloadProgress = new JProgressBar();
    private final LabelTableModel tableModel = new LabelTableModel(this);

    static {
        downloadProgress.setStringPainted(true);
    }

    WikidataInfoLabelPanel() {
        setLayout(new BorderLayout());
        add(new JTable(tableModel), BorderLayout.CENTER);
    }

    void clear() {
        tableModel.clear();
    }

    void downloadLabelsFor(final String qId) {
        tableModel.downloadLabelsFor(qId);
    }

    private synchronized void showDownloadProgress(final String qId) {
        if (!isAncestorOf(downloadProgress)) {
            add(downloadProgress, BorderLayout.NORTH);
        }
        downloadProgress.setIndeterminate(true);
        downloadProgress.setString(I18n.tr("Download labels for {0}…", qId));
        revalidate();
    }

    private synchronized void hideDownloadProgress() {
        remove(downloadProgress);
        revalidate();
    }

    private static class LabelTableModel extends AbstractTableModel {
        private final WikidataInfoLabelPanel parent;
        private String qIdBeingDownloaded;
        private final List<Pair<WbgetentitiesResult.Entity.Label, String>> valueMap = new ArrayList<>();

        LabelTableModel(final WikidataInfoLabelPanel parent) {
            this.parent = parent;
        }

        void clear() {
            this.valueMap.clear();
        }

        void downloadLabelsFor(final String qId) {
            synchronized (valueMap) {
                qIdBeingDownloaded = qId;
                parent.showDownloadProgress(qId);
            }

            new Thread(() -> {
                try {
                    final Map<String, WbgetentitiesResult.Entity.Label> newValues = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels(qId));
                    synchronized (valueMap) {
                        if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                            valueMap.clear();
                            valueMap.addAll(
                                newValues.entrySet().stream()
                                    .map(it -> Pair.create(it.getValue(), it.getKey()))
                                    .sorted(Comparator.comparing(it -> it.a.getLangCode()))
                                    .collect(Collectors.toList())
                            );
                        }
                    }
                } catch (IOException e) {
                    new Notification(I18n.tr("Failed to download labels for {0}!", qId)).setIcon(WikipediaPlugin.W_IMAGE.get()).show();
                }
                parent.hideDownloadProgress();
            }).start();

        }

        @Override
        public int getRowCount() {
            return valueMap.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columnIndex == 0 ? valueMap.get(rowIndex).a : valueMap.get(rowIndex).b;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "language" : "label";
        }
    }
}
