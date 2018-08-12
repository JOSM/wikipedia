// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.data.WikidataEntry;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.OsmPrimitiveUtil;

/**
 * Toggle dialog that displays infos about the currently selected Wikidata item.
 */
public class WikidataInfoToggleDialog extends ToggleDialog {
    private static final Logger L = Logger.getLogger(WikidataInfoToggleDialog.class.getName());
    private static final String EMPTY_STRING = "";

    private final WikipediaToggleDialog wikiDialog;

    private final JPanel mainPanel = new JPanel(new GridLayout(1, 1));

    private final JLabel messageLabel = new JLabel();
    private final JPanel messagePanel = new JPanel(new BorderLayout());

    private final JLabel nameLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel qidLabel = new JLabel();
    private final JPanel infoPanel = new JPanel(new BorderLayout());

    private final JTabbedPane tabs = new JTabbedPane();
    private final WikidataInfoLabelPanel labelTab = new WikidataInfoLabelPanel();
    private final WikidataInfoClaimPanel statementTab = new WikidataInfoClaimPanel();
    private final WikidataInfoSitelinkPanel linkTab = new WikidataInfoSitelinkPanel();

    private final DataSelectionListener selectionListener = it -> updateDisplayedItem();
    private final DataSetListener datasetListener = new DataSetListenerAdapter(it -> {
        if (it.getType() == AbstractDatasetChangedEvent.DatasetEventType.TAGS_CHANGED) {
            updateDisplayedItem();
        }
    });
    private String displayedItem;

    public WikidataInfoToggleDialog(final WikipediaToggleDialog wikiDialog) {
        super(
            I18n.tr("Wikidata Info"),
            "wikidata",
            I18n.tr("Show properties of the selected Wikidata item"),
            null,
            150
        );
        createLayout(mainPanel, false, Collections.emptyList());
        this.wikiDialog = Objects.requireNonNull(wikiDialog);

        messageLabel.setForeground(Color.DARK_GRAY);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.ITALIC));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.add(messageLabel, BorderLayout.CENTER);

        // Set up info panel
        final JPanel basicInfoPanel = new JPanel(new BorderLayout());
        final JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getSize() * 1.25f));
        namePanel.add(nameLabel);
        qidLabel.setForeground(Color.DARK_GRAY);
        qidLabel.setFont(qidLabel.getFont().deriveFont(Font.ITALIC));
        namePanel.add(qidLabel);
        basicInfoPanel.add(namePanel, BorderLayout.CENTER);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN));
        basicInfoPanel.add(descriptionLabel, BorderLayout.SOUTH);
        infoPanel.add(basicInfoPanel, BorderLayout.NORTH);
        infoPanel.add(tabs, BorderLayout.CENTER);

        tabs.add(I18n.tr("Statements"), statementTab);
        tabs.add(I18n.tr("Labels"), labelTab);
        tabs.add(I18n.tr("Links"), linkTab);

        // Set up listeners
        this.wikiDialog.list.addListSelectionListener(event -> updateDisplayedItem());
        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(event -> {
            final DataSet previous = event.getPreviousDataSet();
            final DataSet current = event.getSource().getActiveDataSet();
            if (previous != null) {
                previous.removeDataSetListener(datasetListener);
                previous.removeSelectionListener(selectionListener);
            }
            if (current != null) {
                current.addSelectionListener(selectionListener);
                current.addDataSetListener(datasetListener);
            }
        });
        updateDisplayedItem();
    }

    /**
     * @param qID the Q-ID of the selected wikidata item
     */
    @Override
    public void setTitle(final String qID) {
        super.setTitle(qID == null ? I18n.tr("Wikidata Info") : I18n.tr("Wikidata Info: {0}", qID));
    }

    /**
     * Whenever it is possible that the content of the info panel should be updated, call this method.
     * It checks for the currently selected items in the active dataset and in the Wikidata list. The panel is updated.
     */
    private void updateDisplayedItem() {
        if (!isShowing()) {
            return;
        }
        final DataSet dataset = MainApplication.getLayerManager().getActiveDataSet();
        final Map<String, Integer> wdTagsInDataset =
            dataset == null
            ? Collections.emptyMap()
            : dataset.getSelected().stream()
                .map(OsmPrimitiveUtil::getWikidataValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(it -> it, Collectors.summingInt(it -> 1)));
        if (wdTagsInDataset.isEmpty()) {
            // No OSM objects with valid wikidata=* tags are selected
            final WikipediaEntry entry = this.wikiDialog.list.getSelectedValue();
            if (entry instanceof WikidataEntry) {
                displayItem(entry.article, ((WikidataEntry) entry).label, ((WikidataEntry) entry).description);
            } else {
                displayMessage(null, I18n.tr("No Wikidata item is selected!"));
            }
        } else if (wdTagsInDataset.size() >= 2) {
            // More than one OSM object with valid wikidata=* tag is selected
            final String itemList = wdTagsInDataset.entrySet().stream()
                .map(it -> it.getKey() + " (" + it.getValue() + "×)")
                .collect(Collectors.joining(", "));
            displayMessage(itemList, I18n.tr("More than one OSM object is selected: {0}", itemList));
        } else { // size == 1
            // An OSM object or multiple OSM objects with exactly one valid wikidata=* tag (multiple tags with same value count as one)
            final String qId = wdTagsInDataset.keySet().iterator().next();
            displayItem(qId, EMPTY_STRING, EMPTY_STRING);
        }
    }

    private synchronized void displayItem(final String qId, final String label, final String description) {
        if (qId != null && !qId.equals(getDisplayedItem())) {
            mainPanel.removeAll();
            setTitle(qId);
            nameLabel.setText(label);
            descriptionLabel.setText(description);
            setDisplayedItem(qId);

            labelTab.downloadLabelsFor(qId);
            statementTab.downloadStatementsFor(qId);
            linkTab.downloadSitelinksFor(qId);

            mainPanel.add(infoPanel);
            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }

    private synchronized void displayMessage(final String title, final String message) {
        mainPanel.removeAll();
        setTitle(title);
        setDisplayedItem(null);
        messageLabel.setText(message);
        mainPanel.add(messagePanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void setDisplayedItem(final String qId) {
        this.displayedItem = qId;
        setTitle(qId);
        qidLabel.setText(qId);
    }

    private String getDisplayedItem() {
        return displayedItem;
    }

    @Override
    protected void stateChanged() {
        super.stateChanged();
        updateDisplayedItem();
    }
}
