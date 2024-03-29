// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;

class WikiPreferencesTest {
    @Test
    void testAddGui() {
        final WikiPreferences setting = new WikiPreferences();
        final PreferenceTabbedPane tabPane = new PreferenceTabbedPane();
        tabPane.buildGui();
        int tabs = tabPane.getPluginPreference().getTabPane().getTabCount();
        setting.addGui(tabPane);

        assertEquals(tabs + 1, tabPane.getPluginPreference().getTabPane().getTabCount());
        assertEquals(tabPane.getPluginPreference(), setting.getTabPreferenceSetting(tabPane));

        setting.ok();
    }
}
