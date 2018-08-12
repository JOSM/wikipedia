package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
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
        private final List<TableRow> rows = new ArrayList<>();

        LabelTableModel(final WikidataInfoLabelPanel parent) {
            this.parent = parent;
        }

        void downloadLabelsFor(final String qId) {
            qIdBeingDownloaded = qId;

            new Thread(() -> {
                try {
                    parent.showProgress(I18n.tr("Download labels for {0}…", qId));
                    rows.clear();
                    parent.table.revalidate();
                    parent.revalidate();
                    parent.repaint();
                    final Optional<WbgetentitiesResult.Entity> currentEntity = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels(qId));
                    final Map<String, String> languages = new HashMap<>();
                    try {
                        languages.putAll(ApiQueryClient.query(WikidataActionApiQuery.queryLanguages()));
                    } catch (IOException e) {
                        Logging.warn("Could not download language names! Only the language codes are displayed.", e);
                    }
                    synchronized (rows) {
                        if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                            rows.clear();
                            currentEntity.ifPresent(entity -> {
                                final Map<String, String> labels = entity.getLabels();
                                final Map<String, String> descriptions = entity.getDescriptions();
                                final Map<String, Collection<String>> aliases = entity.getAliases();
                                final Set<String> langCodes = new HashSet<>(labels.keySet());
                                langCodes.addAll(descriptions.keySet());
                                langCodes.addAll(aliases.keySet());
                                langCodes.stream().sorted().forEach(langCode -> {
                                    this.rows.add(new TableRow(
                                        langCode,
                                        languages.get(langCode),
                                        labels.get(langCode),
                                        descriptions.get(langCode),
                                        aliases.get(langCode)
                                    ));
                                });
                            });
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
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return rows.get(rowIndex).langCode;
                case 1: return rows.get(rowIndex).language;
                case 2: return rows.get(rowIndex).label;
                case 3: return rows.get(rowIndex).description;
                case 4:
                default: return String.join(", ", rows.get(rowIndex).aliases);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return I18n.tr("language code");
                case 1: return I18n.tr("language");
                case 2: return I18n.tr("label");
                case 3: return I18n.tr("description");
                case 4:
                default: return I18n.tr("aliases");
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 3;
        }

        private static class TableRow {
            private final String langCode;
            private final String language;
            private final String label;
            private final String description;
            private final Collection<String> aliases;

            TableRow(final String langCode, final String language, final String label, final String description, final Collection<String> aliases) {
                this.langCode = Objects.requireNonNull(langCode);
                this.language = language == null ? langCode : language;
                this.label = label == null ? "" : label;
                this.description = description == null ? "" : description;
                this.aliases = aliases == null ? new ArrayList<>() : aliases;
            }
        }
    }
}
