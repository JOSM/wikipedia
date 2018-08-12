package org.wikipedia.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressJPanel extends JPanel {
    private final JProgressBar progressBar = new JProgressBar();

    ProgressJPanel() {
        setLayout(new BorderLayout());
        progressBar.setStringPainted(true);
    }

    protected synchronized void showProgress(final String message) {
        if (!isAncestorOf(progressBar)) {
            add(progressBar, BorderLayout.NORTH);
        }
        progressBar.setIndeterminate(true);
        progressBar.setString(message);
        revalidate();
        repaint();
    }

    protected synchronized void hideProgress() {
        remove(progressBar);
        revalidate();
        repaint();
    }
}
