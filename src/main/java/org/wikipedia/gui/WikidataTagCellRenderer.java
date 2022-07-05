// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.data.WikidataEntry;
import org.wikipedia.tools.RegexUtil;

public class WikidataTagCellRenderer extends DefaultTableCellRenderer {

    final Map<String, CompletableFuture<String>> labelCache = new ConcurrentHashMap<>();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column != 1
                || !(value instanceof Map<?, ?> && ((Map<?, ?>) value).size() == 1)) {
            return null;
        }
        final String key = table.getValueAt(row, 0).toString();
        if (!("wikidata".equals(key) || (key != null && key.endsWith(":wikidata")))) {
            return null;
        }

        // Determine a suitable color between background and foreground to render labels.
        // That way they will be legible even when selected.
        final Color background = isSelected ? table.getSelectionBackground() : table.getBackground();
        final Color foreground = isSelected ? table.getSelectionForeground() : table.getForeground();
        final String labelColor = blend(background, foreground);

        final String id = ((Map<?, ?>) value).keySet().iterator().next().toString();
        final JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (RegexUtil.isValidQId(id)) {
            return renderValues(Collections.singleton(id), table, component, labelColor);
        } else if (id.contains(";")) {
            final List<String> ids = Arrays.asList(id.split("\\s*;\\s*"));
            if (ids.stream().allMatch(RegexUtil::isValidQId)) {
                return renderValues(ids, table, component, labelColor);
            }
        }
        return null;
    }

    protected JLabel renderValues(Collection<String> ids, JTable table, JLabel component, String labelColor) {

        ids.forEach(id ->
                labelCache.computeIfAbsent(id, x ->
                        CompletableFuture.supplyAsync(() -> WikipediaApp.getLabelForWikidata(x, Locale.getDefault()),
                        // See #16204#comment:10 - Don't use ForkJoinPool#commonPool(), does not work with WebStart
                        Utils.newForkJoinPool("wikipedia.wikidata.renderer.numberOfThreads", "wikidata-renderer-%d", Thread.NORM_PRIORITY)))
        );

        final Collection<String> texts = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (!labelCache.get(id).isDone()) {
                labelCache.get(id).thenRun(() -> GuiHelper.runInEDT(table::repaint));
                return null;
            }
            final String label;
            try {
                label = labelCache.get(id).get();
            } catch (InterruptedException | ExecutionException e) {
                Logging.log(Level.WARNING, "Could not fetch Wikidata label for " + id, e);
                return null;
            }
            if (label == null) {
                return null;
            }
            texts.add(WikidataEntry.getLabelText(id, label, labelColor));
        }
        component.setText("<html>" + String.join("; ", texts));
        component.setToolTipText("<html>" + Utils.joinAsHtmlUnorderedList(texts));
        return component;
    }

    /**
     * Blends two RGB colors to mimic a semi-transparent foreground
     * color and returns a CSS/HTML color string.
     */
    private static String blend(Color background, Color foreground) {
        int red = (background.getRed() + foreground.getRed()) / 2;
        int green = (background.getGreen() + foreground.getGreen()) / 2;
        int blue = (background.getBlue() + foreground.getBlue()) / 2;
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
