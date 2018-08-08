package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;

/**
 * Panel displaying the labels for a Wikidata item
 */
class WikidataInfoLabelPanel extends ProgressJPanel {
    private final LabelTableModel tableModel = new LabelTableModel(this);
    private final JTable table = new JTable(tableModel);

    WikidataInfoLabelPanel() {
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void downloadLabelsFor(final String qId) {
        tableModel.downloadLabelsFor(qId);
    }

    private static class LabelTableModel extends AbstractTableModel {
        private final WikidataInfoLabelPanel parent;
        private String qIdBeingDownloaded;
        private final List<Pair<WbgetentitiesResult.Entity.Label, String>> valueMap = new ArrayList<>();

        LabelTableModel(final WikidataInfoLabelPanel parent) {
            this.parent = parent;
        }

        void downloadLabelsFor(final String qId) {
            qIdBeingDownloaded = qId;

            new Thread(() -> {
                try {
                    parent.showProgress(I18n.tr("Download labels for {0}…", qId));
                    valueMap.clear();
                    parent.table.revalidate();
                    parent.revalidate();
                    parent.repaint();
                    final Map<String, WbgetentitiesResult.Entity.Label> newValues = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels(qId));
                    final Map<String, String> languages = new HashMap<>();
                    try {
                        languages.putAll(ApiQueryClient.query(WikidataActionApiQuery.queryLanguages()));
                    } catch (IOException e) {
                        Logging.warn("Could not download language names! Only the language codes are displayed.", e);
                    }
                    synchronized (valueMap) {
                        if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                            valueMap.clear();
                            valueMap.addAll(
                                newValues.values().stream()
                                    .map(it -> Pair.create(it, languages.containsKey(it.getLangCode()) ? languages.get(it.getLangCode()) : it.getLangCode()))
                                    .sorted(Comparator.comparing(it -> it.a.getLangCode()))
                                    .collect(Collectors.toList())
                            );
                            parent.table.revalidate();
                        }
                    }
                } catch (IOException e) {
                    new Notification(I18n.tr("Failed to download labels for {0}!", qId)).setIcon(WikipediaPlugin.W_IMAGE.get()).show();
                }
                parent.hideProgress();
            }).start();
        }

        @Override
        public int getRowCount() {
            return valueMap.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return valueMap.get(rowIndex).a.getLangCode();
                case 1: return valueMap.get(rowIndex).b;
                case 2:
                default: return valueMap.get(rowIndex).a.getValue();
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return I18n.tr("language code");
                case 1: return I18n.tr("language");
                case 2:
                default: return I18n.tr("label");
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3;
        }
    }
}
