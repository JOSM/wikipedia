package org.wikipedia.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressJPanel extends JPanel {
    private final JProgressBar PROGRESS_BAR = new JProgressBar();

    ProgressJPanel() {
        setLayout(new BorderLayout());
        PROGRESS_BAR.setStringPainted(true);
    }

    protected synchronized void showProgress(final String message) {
        if (!isAncestorOf(PROGRESS_BAR)) {
            add(PROGRESS_BAR, BorderLayout.NORTH);
        }
        PROGRESS_BAR.setIndeterminate(true);
        PROGRESS_BAR.setString(message);
        revalidate();
        repaint();
    }

    protected synchronized void hideProgress() {
        remove(PROGRESS_BAR);
        revalidate();
        repaint();
    }
}
