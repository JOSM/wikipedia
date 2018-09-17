// License: GPL. For details, see LICENSE file.
package org.wikipedia.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.WikipediaApp;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.WikiProperties;

public class WikipediaAddNamesAction extends JosmAction {

    public WikipediaAddNamesAction() {
        super(tr("Add names from Wikipedia"), "dialogs/wikipedia",
                tr("Fetches interwiki links from Wikipedia in order to add several name tags"),
                null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final WikipediaEntry wp = WikipediaEntry.parseTag("wikipedia", getWikipediaValue());
        if (wp == null) {
            new Notification(I18n.tr("Could not add names. Wikipedia tag is not recognized!"))
                .setIcon(WikipediaPlugin.W_IMAGE.setMaxSize(ImageProvider.ImageSizes.LARGEICON).get())
                .show();
        } else {
            List<String[]> tags = WikipediaApp.forLanguage(wp.lang).getInterwikiArticles(wp.article).stream()
                .filter(this::useWikipediaLangArticle)
                .map(i -> new String[]{"name:" + i.lang, i.article})
                .collect(Collectors.toList());
            if (Logging.isDebugEnabled()) {
                Logging.debug(tags.toString());
            }
            AddTagsDialog.addTags(tags.toArray(new String[tags.size()][]), "Wikipedia", getLayerManager().getEditDataSet().getSelected());
        }
    }

    private boolean useWikipediaLangArticle(WikipediaEntry i) {
        return
            (!WikiProperties.FILTER_ISO_LANGUAGES.get() || Arrays.asList(Locale.getISOLanguages()).contains(i.lang)) &&
            (!WikiProperties.FILTER_SAME_NAMES.get() || !i.article.equals(getLayerManager().getEditDataSet().getSelected().iterator().next().get("name")));
    }

    private String getWikipediaValue() {
        final DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null || ds.getSelected() == null || ds.getSelected().size() != 1) {
            return null;
        }
        return ds.getSelected().iterator().next().get("wikipedia");
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getWikipediaValue() != null);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledState();
    }
}
