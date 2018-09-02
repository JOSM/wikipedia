// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class WikiPreferencesTest {

    @Rule
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testAddGui() {
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
