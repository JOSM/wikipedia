// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.I18n;
import org.wikipedia.io.SophoxDownloadReader;
import org.wikipedia.tools.WikiProperties;

public class WikiPreferences implements SubPreferenceSetting {

    private final JLabel languageLabel = new JLabel(I18n.tr("Wikipedia language"), JLabel.TRAILING);
    private final JTextField languageField = new JTextField();
    private final JLabel sophoxServerLabel = new JLabel(I18n.tr("Sophox server"), JLabel.TRAILING);
    private final HistoryComboBox sophoxServerField = new HistoryComboBox();

    public WikiPreferences() {
        super();
        languageLabel.setToolTipText(I18n.tr("Sets the default language for the Wikipedia articles"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        final JPanel container = new JPanel(new GridBagLayout());

        ExpertToggleAction.addExpertModeChangeListener(isExpert -> {
            sophoxServerLabel.setVisible(isExpert);
            sophoxServerField.setVisible(isExpert);
            container.revalidate();
            container.repaint();
        }, true);

        container.setAlignmentY(JPanel.TOP_ALIGNMENT);
        final GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = .1;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 10, 5, 10);
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(languageLabel, constraints);
        constraints.gridx++;
        constraints.weightx = 1;
        container.add(languageField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.weightx = .1;
        container.add(sophoxServerLabel, constraints);
        constraints.gridx++;
        constraints.weightx = 1;
        container.add(sophoxServerField, constraints);

        constraints.gridy++;
        constraints.weighty = 1;
        container.add(Box.createVerticalGlue(), constraints);

        languageField.setText(WikiProperties.WIKIPEDIA_LANGUAGE.get());
        sophoxServerField.setPossibleItems(SophoxDownloadReader.SOPHOX_SERVER_HISTORY.get());
        sophoxServerField.setText(SophoxDownloadReader.SOPHOX_SERVER.get());

        getTabPreferenceSetting(gui).addSubTab(this, "Wikipedia", container);
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getPluginPreference();
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public boolean ok() {
        WikiProperties.WIKIPEDIA_LANGUAGE.put(languageField.getText());
        SophoxDownloadReader.SOPHOX_SERVER_HISTORY.put(sophoxServerField.getHistory());
        SophoxDownloadReader.SOPHOX_SERVER.put(sophoxServerField.getText());
        return false;
    }
}
