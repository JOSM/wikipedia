// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.WikiProperties;

public class WikiLayer extends Layer implements ListDataListener {
    private static final Icon LAYER_ICON = new ImageProvider("w").setMaxSize(ImageProvider.ImageSizes.LAYER).get();

    private static final Color MARKER_FILL_COLOR = new Color(153, 0, 0, 180);
    private static final Color MARKER_STROKE_COLOR = new Color(255, 255, 255);
    private static final double MIN_MARKER_HEIGHT = 10.0;

    private static double markerHeight = Math.max(MIN_MARKER_HEIGHT, WikiProperties.WIKI_LAYER_MARKER_HEIGHT.get());

    static {
        WikiProperties.WIKI_LAYER_MARKER_HEIGHT.addListener(it -> markerHeight = Math.max(MIN_MARKER_HEIGHT, it.getProperty().get()));
    }

    private final WikipediaToggleDialog wikiDialog;
    public WikiLayer(final WikipediaToggleDialog wikiDialog) {
        super("WikiLayer");
        this.wikiDialog = wikiDialog;
        wikiDialog.model.addListDataListener(this);
    }

    @Override
    public Icon getIcon() {
        return LAYER_ICON;
    }

    @Override
    public Object getInfoComponent() {
        return MessageFormat.format("{0} elements with coordinates, {1} elements with missing coordinates are not displayed", wikiDialog.model.getSize(), Collections.list(wikiDialog.model.elements()).stream().filter(it -> it.coordinate == null).count());
    }

    @Override
    public String getToolTipText() {
        return I18n.tr("The Wikipedia/Wikidata layer");
    }

    @Override
    public void mergeFrom(Layer from) {
        throw new UnsupportedOperationException(MessageFormat.format("Layer of type {0} is not mergable!", WikiLayer.class.getSimpleName()));
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (WikipediaEntry entry : Collections.list(wikiDialog.model.elements())) {
            v.visit(entry.coordinate);
        }
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[]{new LayerListPopup.InfoAction(this)};
    }

    @Override
    public void paint(final Graphics2D g, final MapView mv, final Bounds bbox) {
        // Expand bbox to also paint markers that are only partially visible (marker is centered above the location, 20px wide, 30px tall)
        final Point maxPoint = mv.getPoint(bbox.getMax());
        bbox.extend(mv.getLatLon(maxPoint.getX() + 10, maxPoint.getY() + 30));
        final Point minPoint = mv.getPoint(bbox.getMin());
        bbox.extend(mv.getLatLon(minPoint.getX() - 10, minPoint.getY() + 30));

        final Collection<Point> entriesInBbox = Collections.list(wikiDialog.model.elements()).parallelStream()
            .filter(it -> it.coordinate != null && bbox.contains(it.coordinate))
            .map(it -> mv.getPoint(it.coordinate))
            .collect(Collectors.toList());
        paintWikiMarkers(g, entriesInBbox);
    }

    private static void paintWikiMarkers(final Graphics2D g, final Collection<Point> points) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setStroke(new BasicStroke(2));
        for (final Point point: points) {
            final Path2D path = new Path2D.Double();
            path.moveTo(point.getX(), point.getY());
            path.append(new Arc2D.Double(point.getX() - markerHeight / 3, point.getY() - markerHeight, markerHeight / 3 * 2, markerHeight / 3 * 2, -30, 240.0, Arc2D.OPEN), true);
            path.closePath();

            g.setColor(MARKER_FILL_COLOR);
            g.fill(path);
            g.setColor(MARKER_STROKE_COLOR);
            g.draw(path);
        }
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        invalidate();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        invalidate();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        invalidate();
    }
}
