// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.util.concurrent.Executors;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.tools.Debouncer;

abstract class WikiSearchTextResultListPanel<T> extends SearchTextResultListPanel<T> {

    protected final Debouncer debouncer = new Debouncer(
            Executors.newSingleThreadScheduledExecutor(Utils.newThreadFactory("wikipedia-search-%d", Thread.NORM_PRIORITY)));

    public T getSelectedItem() {
        synchronized (lsResultModel) {
            final int idx = lsResult.getSelectedIndex();
            final T selected = idx < lsResultModel.getSize() && idx >= 0 ? lsResultModel.getElementAt(idx) : null;

            if (selected != null) {
                return selected;
            } else if (!lsResultModel.isEmpty()) {
                return lsResultModel.getElementAt(0);
            } else {
                return null;
            }
        }
    }
}
