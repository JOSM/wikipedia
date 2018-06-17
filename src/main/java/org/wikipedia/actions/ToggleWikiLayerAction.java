// License: GPL. For details, see LICENSE file.
package org.wikipedia.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.wikipedia.gui.WikiLayer;
import org.wikipedia.gui.WikipediaToggleDialog;

public class ToggleWikiLayerAction extends JosmAction {
    private static final ImageProvider ICON_ADDWIKI = new ImageProvider("layer", "addwiki").setMaxSize(ImageProvider.ImageSizes.SIDEBUTTON);
    private static final ImageProvider ICON_REMOVEWIKI = new ImageProvider("layer", "removewiki").setMaxSize(ImageProvider.ImageSizes.SIDEBUTTON);

    private final WikipediaToggleDialog dialog;

    public ToggleWikiLayerAction(final WikipediaToggleDialog dialog) {
        super(
            I18n.tr("Wiki layer"),
            ICON_ADDWIKI,
            I18n.tr("Toggle the layer displaying Wikipedia articles or Wikidata items"),
            null,
            false,
            null,
            true
        );
        this.dialog = dialog;
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        final List<WikiLayer> wikiLayers = MainApplication.getLayerManager().getLayersOfType(WikiLayer.class);
        if (wikiLayers.size() <= 0) {
            MainApplication.getLayerManager().addLayer(new WikiLayer(dialog));
        } else {
            for (WikiLayer layer : wikiLayers) {
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    public void updateEnabledState() {
        final ImageProvider provider;
        if (MainApplication.getLayerManager().getLayersOfType(WikiLayer.class).size() <= 0) {
            provider = ICON_ADDWIKI;
        } else {
            provider = ICON_REMOVEWIKI;
        }
        provider.getResource().attachImageIcon(this, true);
    }
}
