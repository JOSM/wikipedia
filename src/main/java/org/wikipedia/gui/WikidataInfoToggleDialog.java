// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.data.WikidataEntry;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.OsmPrimitiveUtil;

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
    private final JPanel labelTab = new JPanel();
    private final JPanel statementTab = new JPanel();
    private final JPanel linkTab = new JPanel();
    private final JButton webLinkButton = new JButton();

    private final DataSelectionListener selectionListener = it -> updateSelection();
    private final DataSetListener datasetListener = new DataSetListenerAdapter(it -> {
        if (it.getType() == AbstractDatasetChangedEvent.DatasetEventType.TAGS_CHANGED) {
            updateSelection();
        }
    });

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

        // Set up statement tab
        statementTab.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(1, 1, 1, 1);
        constraints.fill = GridBagConstraints.BOTH;
        // At the moment only dummy content
        constraints.weightx = 1;
        statementTab.add(new StatementPanel("instance of", "example", "dummy content"), constraints);
        constraints.gridy++;
        statementTab.add(new StatementPanel("start date", "42"), constraints);
        constraints.gridy++;
        statementTab.add(new StatementPanel("architect", "John Doe"), constraints);
        constraints.gridy++;
        constraints.weighty = 1;
        statementTab.add(GBC.glue(0, 0), constraints);

        linkTab.add(webLinkButton);

        tabs.add(I18n.tr("Statements"), statementTab);
        tabs.add(I18n.tr("Labels"), labelTab);
        tabs.add(I18n.tr("Links"), linkTab);

        // Set up listeners
        this.wikiDialog.list.addListSelectionListener(event -> updateSelection());
        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(event -> {
            System.out.println("Fire active layer change");
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
        updateSelection();
    }

    /**
     * @param qID the Q-ID of the selected wikidata item
     */
    @Override
    public void setTitle(final String qID) {
        super.setTitle(qID == null ? I18n.tr("Wikidata info") : I18n.tr("Wikidata info: {0}", qID));
    }

    /**
     * Whenever it is possible that the content of the info panel should be updated, call this method.
     * It checks for the currently selected items in the active dataset and in the Wikidata list. The panel is updated.
     */
    private void updateSelection() {
        mainPanel.removeAll();
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
            final WikipediaEntry entry = this.wikiDialog.list.getSelectedValue();
            if (entry instanceof WikidataEntry) {
                setTitle(entry.article);
                nameLabel.setText(((WikidataEntry) entry).label);
                descriptionLabel.setText(((WikidataEntry) entry).description);
                qidLabel.setText(entry.article);
                mainPanel.add(new JScrollPane(infoPanel));
            } else {
                setTitle(null);
                messageLabel.setText(I18n.tr("No Wikidata item is selected!"));
                mainPanel.add(messagePanel);
            }
        } else if (wdTagsInDataset.size() >= 2) {
            final String itemList = wdTagsInDataset.entrySet().stream().map(it -> it.getKey() + " (" + it.getValue() + "×)").collect(Collectors.joining(", "));
            setTitle(itemList);
            messageLabel.setText(I18n.tr("More than one OSM object is selected: {0}", itemList));
            mainPanel.add(messagePanel);
        } else { // size == 1
            final String qId = wdTagsInDataset.keySet().iterator().next();
            setTitle(qId);
            nameLabel.setText(EMPTY_STRING);
            descriptionLabel.setText(EMPTY_STRING);
            qidLabel.setText(qId);
            mainPanel.add(new JScrollPane(infoPanel));
            webLinkButton.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String uri = "https://www.wikidata.org/wiki/" + qId;
                    final String error = OpenBrowser.displayUrl(uri);
                    if (error != null) {
                        new Notification(I18n.tr("Can't open website {0} in browser! Error message: {1}", uri, error)).setIcon(WikipediaPlugin.W_IMAGE.get()).show();
                    }
                }
            });
            webLinkButton.setText(I18n.tr("Open item {0} in browser", qId));
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}
