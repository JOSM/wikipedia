// License: GPL. For details, see LICENSE file.
package org.wikipedia;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.ImageProvider;
import org.wikipedia.actions.FetchWikidataAction;
import org.wikipedia.actions.WikipediaAddNamesAction;
import org.wikipedia.actions.WikipediaCopyTemplate;
import org.wikipedia.gui.SophoxDownloadReader;
import org.wikipedia.gui.WikiPreferences;
import org.wikipedia.gui.WikidataInfoToggleDialog;
import org.wikipedia.gui.WikidataItemSearchDialog;
import org.wikipedia.gui.WikidataTagCellRenderer;
import org.wikipedia.gui.WikipediaToggleDialog;
import org.wikipedia.validator.UnusualWikidataClasses;
import org.wikipedia.validator.WikidataItemExists;
import org.wikipedia.validator.WikipediaAgainstWikidata;
import org.wikipedia.validator.WikipediaRedirect;

public final class WikipediaPlugin extends Plugin {
    public static final ImageIcon LOGO = ImageProvider.get("dialogs/wikipedia");
    public static final ImageProvider W_IMAGE = new ImageProvider("w");

    private static String name;
    private static String versionInfo;

    private final PreferenceSetting preferences = new WikiPreferences();

    public WikipediaPlugin(PluginInformation info) {
        super(info);
        versionInfo = String.format("JOSM/%s JOSM-wikipedia/%s", Version.getInstance().getVersionString(), info.version);
        name = info.name;
        new WikipediaCopyTemplate();
        JMenu dataMenu = MainApplication.getMenu().dataMenu;
        MainMenu.add(dataMenu, new WikipediaAddNamesAction());
        MainMenu.add(dataMenu, new FetchWikidataAction());
        MainMenu.add(dataMenu, new WikidataItemSearchDialog.Action());

        DownloadDialog.addDownloadSource(new SophoxDownloadReader());

        OsmValidator.addTest(WikidataItemExists.class);
        OsmValidator.addTest(WikipediaAgainstWikidata.class);
        OsmValidator.addTest(WikipediaRedirect.class);
        OsmValidator.addTest(UnusualWikidataClasses.class);
    }

    public static String getVersionInfo() {
        return versionInfo;
    }

    public static String getName() {
        return name;
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null) {
            final WikipediaToggleDialog wikiDialog = new WikipediaToggleDialog();
            newFrame.addToggleDialog(wikiDialog);
            newFrame.addToggleDialog(new WikidataInfoToggleDialog(wikiDialog));
            newFrame.propertiesDialog.addCustomPropertiesCellRenderer(new WikidataTagCellRenderer());
        }
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return preferences;
    }
}
