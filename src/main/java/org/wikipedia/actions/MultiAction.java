// License: GPL. For details, see LICENSE file.
package org.wikipedia.actions;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.Logging;

public class MultiAction extends JosmAction {

    public static SideButton createButton(final String name, final String iconName, final String tooltip, final Action... actions) {
        final MultiAction popupAction = new MultiAction(name, iconName, tooltip, actions);
        final SideButton button = new SideButton(popupAction);
        popupAction.setParent(button);
        button.createArrow(it -> button.getAction().actionPerformed(it));
        return button;
    }

    private final JPopupMenu popup;
    private Component parent;

    private MultiAction(final String name, final String iconName, final String tooltip, final Action... actions) {
        // I18n: {0} is the number of available sources
        super(name, iconName, tooltip, null, false, false);
        popup = new ActionsPopup(actions);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Component finalParent = parent;
        if (finalParent != null) {
            final Rectangle parentBounds = finalParent.getBounds();
            popup.show(finalParent, parentBounds.x - parentBounds.width, parentBounds.y);
        } else {
            Logging.error("The parent component of a MultiAction must not be null!");
        }
    }

    private void setParent(final Component parent) {
        this.parent = parent;
    }

    private static class ActionsPopup extends JPopupMenu {
        private ActionsPopup(final Action... actions) {
            for (final Action action : actions) {
                add(new JMenuItem(action));
            }
        }
    }
}
