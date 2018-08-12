package org.wikipedia.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

class StatementPanel extends JPanel {
    private final JLabel propertyLabel = new JLabel();
    private final List<JLabel> valueLabels = new ArrayList<>();
    private final GridBagConstraints constraints = new GridBagConstraints();

    StatementPanel(final String propertyName, final String... valueNames) {
        initLayout();
        setPropertyName(propertyName);
        setValueNames(valueNames);
    }

    private void initLayout() {
        setBorder(new LineBorder(new Color(0xc8ccd1), 1));
        propertyLabel.setFont(propertyLabel.getFont().deriveFont(Font.BOLD));
        propertyLabel.setForeground(Color.BLACK);
        propertyLabel.setBackground(new Color(0xeaecf0));
        propertyLabel.setHorizontalAlignment(JLabel.CENTER);
        propertyLabel.setOpaque(true);
        propertyLabel.setHorizontalAlignment(JLabel.TRAILING);
        setBackground(new Color(0xeaecf0));
        setLayout(new GridBagLayout());

        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(propertyLabel, constraints);
    }

    private void setPropertyName(final String propertyName) {
        this.propertyLabel.setText(propertyName);
    }

    private synchronized void setValueNames(final String[] valueNames) {
        final Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridx = 1;
        constraints.weightx = 1;
        for (int i = 0; i < Math.max(valueNames.length, valueLabels.size()); i++) {
            if (i >= valueNames.length) {
                final JLabel currentLabel = valueLabels.get(i);
                remove(currentLabel);
                valueLabels.remove(currentLabel);
            } else if (i >= valueLabels.size()) {
                final JLabel currentLabel = new JLabel(valueNames[i], JLabel.LEADING);
                currentLabel.setFont(currentLabel.getFont().deriveFont(Font.PLAIN));
                currentLabel.setBackground(Color.WHITE);
                currentLabel.setForeground(Color.BLACK);
                currentLabel.setOpaque(true);
                currentLabel.setBorder(padding);
                valueLabels.add(currentLabel);
                constraints.gridy = i;
                add(currentLabel, constraints);
            } else {
                final JLabel currentLabel = valueLabels.get(i);
                currentLabel.setText(valueNames[i]);
            }
        }
    }
}
