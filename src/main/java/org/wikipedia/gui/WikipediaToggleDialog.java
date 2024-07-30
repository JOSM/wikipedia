// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent.DatasetEventType;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.actions.FetchWikidataAction;
import org.wikipedia.actions.MultiAction;
import org.wikipedia.actions.ToggleWikiLayerAction;
import org.wikipedia.data.WikipediaEntry;
import org.wikipedia.tools.ListUtil;
import org.wikipedia.tools.WikiProperties;

public class WikipediaToggleDialog extends ToggleDialog implements ActiveLayerChangeListener, DataSetListenerAdapter.Listener {

    /** A string describing the context (use-case) for determining the dialog title */
    String titleContext;
    final Set<String> articles = new HashSet<>();
    final DefaultListModel<WikipediaEntry> model = new DefaultListModel<>();
    final JList<WikipediaEntry> list = new JList<>(model);

    public WikipediaToggleDialog() {
        super(
            tr("Wikipedia"),
            "wikipedia",
            tr("Fetch Wikipedia articles with coordinates"),
            null,
            150,
            true,
            WikiPreferences.class
        );

        final Action[] downloadActions = {
            new WikipediaLoadCoordinatesAction(false),
            new WikipediaLoadCoordinatesAction(true),
            new WikipediaLoadCategoryAction()
        };
        listSetup(list);
        createLayout(list, true, Arrays.asList(
                new SideButton(new ToggleWikiLayerAction(this)),
                MultiAction.createButton(
                    I18n.tr("Download elements"),
                    "download",
                    I18n.tr("Download all elements from one of {0} sources", downloadActions.length),
                    downloadActions
                ),
                new SideButton(new PasteWikipediaArticlesAction()),
                new SideButton(new AddWikipediaTagAction(list))
        ));
        updateTitle();
    }

    private void listSetup(JList<WikipediaEntry> list) {
        list.setToolTipText(tr("Double click on item to search for object with article name (and center coordinate)"));
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null && MainApplication.getLayerManager().getEditDataSet() != null) {
                    final WikipediaEntry entry = list.getSelectedValue();
                    if (entry.coordinate != null) {
                        BoundingXYVisitor bbox = new BoundingXYVisitor();
                        bbox.visit(entry.coordinate);
                        MainApplication.getMap().mapView.zoomTo(bbox);
                    }
                    final String search = entry.getSearchText().replaceAll("\\(.*\\)", "");
                    SearchAction.search(search, SearchMode.replace);
                }
            }
        });

        list.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public JLabel getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final WikipediaEntry entry = (WikipediaEntry) value;
                final String labelText = "<html>" + entry.getLabelText();
                final JLabel label = (JLabel) super.getListCellRendererComponent(list, labelText, index, isSelected, cellHasFocus);
                if (entry.getWiwosmStatus() != null && entry.getWiwosmStatus()) {
                    label.setIcon(ImageProvider.getIfAvailable("misc", "grey_check"));
                    label.setToolTipText(/* I18n: WIWOSM server already links Wikipedia article to object/s */ tr("Available via WIWOSM server"));
                } else if (articles.contains(entry.article)) {
                    label.setIcon(ImageProvider.getIfAvailable("misc", "green_check"));
                    label.setToolTipText(/* I18n: object/s from dataset contain link to Wikipedia article */ tr("Available in local dataset"));
                } else {
                    label.setToolTipText(tr("Not linked yet"));
                }
                return label;
            }
        });

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new OpenWikipediaArticleAction());
        popupMenu.add(new ZoomToWikipediaArticleAction());
        list.setComponentPopupMenu(popupMenu);
    }

    private void updateTitle() {
        final WikipediaApp app = newWikipediaApp();
        try {
            final URL url = new URL(app.getSiteUrl());
            if (titleContext == null) {
                setTitle(url.getHost());
            } else {
                setTitle(url.getHost() + ": " + titleContext);
            }
        } catch (MalformedURLException e) {
            Logging.warn("The site URL {0} is malformed!", app.getSiteUrl());
            setTitle(tr("{0} (malformed site URL)"));
        }
    }

    private WikipediaApp newWikipediaApp() {
        try {
            return WikipediaApp.forLanguage(list.getModel().getElementAt(0).lang);
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            Logging.trace(arrayIndexOutOfBoundsException);
            return WikipediaApp.forLanguage(WikiProperties.WIKIPEDIA_LANGUAGE.get());
        }
    }

    class WikipediaLoadCoordinatesAction extends AbstractAction {

        private final boolean wikidata;

        WikipediaLoadCoordinatesAction(boolean wikidata) {
            super(
                wikidata
                ? tr("Wikidata items in viewport")
                : tr("Wikipedia articles in viewport (for language {0})", WikiProperties.WIKIPEDIA_LANGUAGE.get())
            );
            this.wikidata = wikidata;
            new ImageProvider("dialogs", wikidata ? "wikidata" : "wikipedia").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, wikidata
                    ? tr("Fetches all coordinates from Wikidata in the current view")
                    : tr("Fetches all coordinates from Wikipedia in the current view"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                // determine bbox
                MapView mapView = MainApplication.getMap().mapView;
                final LatLon min = mapView.getLatLon(0, mapView.getHeight());
                final LatLon max = mapView.getLatLon(mapView.getWidth(), 0);
                // add entries to list model
                titleContext = tr("coordinates");
                updateTitle();
                new UpdateWikipediaArticlesSwingWorker() {

                    @Override
                    List<WikipediaEntry> getEntries() {
                        return WikipediaApp.forLanguage(wikidata ? "wikidata" : WikiProperties.WIKIPEDIA_LANGUAGE.get())
                                .getEntriesFromCoordinates(min, max);
                    }
                }.execute();
            } catch (Exception ex) {
                throw new JosmRuntimeException(ex);
            }
        }
    }

    abstract class UpdateWikipediaArticlesSwingWorker extends SwingWorker<Void, WikipediaEntry> {

        abstract List<WikipediaEntry> getEntries();

        @Override
        protected Void doInBackground() throws Exception {
            final List<WikipediaEntry> entries = getEntries();
            entries.sort(null);
            publish(entries.toArray(new WikipediaEntry[0]));
            ListUtil.processInBatches(entries, 20, batch -> {
                WikipediaApp.forLanguage(batch.get(0).lang).updateWIWOSMStatus(batch);
                list.repaint();
            });
            return null;
        }

        @Override
        protected void process(List<WikipediaEntry> chunks) {
            model.clear();
            chunks.forEach(model::addElement);
            updateTitle();
            updateWikipediaArticles();
        }

    }

    class WikipediaLoadCategoryAction extends AbstractAction {

        WikipediaLoadCategoryAction() {
            super(tr("all Wikipedia articles in Category"));
            new ImageProvider("data", "sequence").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Fetches a list of all Wikipedia articles of a category"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final WikipediaCategorySearchDialog categorySearchDialog = WikipediaCategorySearchDialog.getInstance();
            categorySearchDialog.showDialog();
            if (categorySearchDialog.getValue() != 1) {
                return;
            }
            final String category = categorySearchDialog.getCategory();
            if (category == null) {
                return;
            }

            titleContext = category;
            updateTitle();

            new UpdateWikipediaArticlesSwingWorker() {
                @Override
                List<WikipediaEntry> getEntries() {
                    return WikipediaApp
                        .forLanguage(WikiProperties.WIKIPEDIA_LANGUAGE.get())
                        .getEntriesFromCategory(category, WikiProperties.WIKIPEDIA_DEPTH.get());
                }
            }.execute();
        }
    }

    class PasteWikipediaArticlesAction extends AbstractAction {

        PasteWikipediaArticlesAction() {
            super(tr("Paste Wikipedia articles"));
            new ImageProvider("paste").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Pastes Wikipedia articles from the system clipboard"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            titleContext = tr("clipboard");
            updateTitle();
            new UpdateWikipediaArticlesSwingWorker() {

                @Override
                List<WikipediaEntry> getEntries() {
                    return WikipediaApp.getEntriesFromClipboard(WikiProperties.WIKIPEDIA_LANGUAGE.get(), ClipboardUtils.getClipboardStringContent());
                }
            }.execute();
        }
    }

    class OpenWikipediaArticleAction extends AbstractAction {

        OpenWikipediaArticleAction() {
            super(tr("Open Article"));
            new ImageProvider("browser").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Opens the Wikipedia article of the selected item in a browser"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Optional<String> url = Optional.ofNullable(list.getSelectedValue()).flatMap(WikipediaEntry::getBrowserUrl);
            if (url.isPresent()) {
                Logging.info("Wikipedia: opening " + url.get());
                OpenBrowser.displayUrl(url.get());
            }
        }
    }

    static class AddWikipediaTagAction extends AbstractAction {

        private final JList<WikipediaEntry> list;

        AddWikipediaTagAction(JList<WikipediaEntry> list) {
            super(tr("Add Tag"));
            this.list = list;
            new ImageProvider("pastetags").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Adds a ''wikipedia'' tag corresponding to this article to the selected objects"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addTag(list.getSelectedValue());
        }

        static void addTag(WikipediaEntry entry) {
            if (entry == null) {
                return;
            }
            addTag(entry.createWikipediaTag());
        }

        static void addTag(Tag tag) {
            final DataSet editDataSet = MainApplication.getLayerManager().getEditDataSet();
            if (tag == null || editDataSet == null) {
                return;
            }
            final Collection<OsmPrimitive> selected = editDataSet.getSelected();
            if (selected.isEmpty() || !GuiUtils.confirmOverwrite(tag.getKey(), tag.getValue(), selected)) {
                return;
            }
            ChangePropertyCommand cmd = new ChangePropertyCommand(
                    selected,
                    tag.getKey(), tag.getValue());
            UndoRedoHandler.getInstance().add(cmd);
            MainApplication.worker.execute(new FetchWikidataAction.Fetcher(selected));
        }
    }

    class ZoomToWikipediaArticleAction extends AbstractAction {

        ZoomToWikipediaArticleAction() {
            super(tr("Zoom to selection"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Zoom to selection"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final WikipediaEntry entry = list.getSelectedValue();
            if (entry == null) {
                return;
            }
            final LatLon latLon = entry.coordinate != null
                    ? entry.coordinate
                    : WikipediaApp.forLanguage(entry.lang).getCoordinateForArticle(entry.article);
            if (latLon == null) {
                return;
            }
            MainApplication.getMap().mapView.zoomTo(latLon);
        }
    }

    protected void updateWikipediaArticles() {
        final WikipediaApp app = newWikipediaApp();
        articles.clear();
        final DataSet editDataset = MainApplication.getLayerManager().getEditDataSet();
        if (editDataset != null) {
            editDataset.allPrimitives().stream()
                .flatMap(app::getWikipediaArticles)
                .forEach(articles::add);
        }
    }

    private final DataSetListenerAdapter dataChangedAdapter = new DataSetListenerAdapter(this);

    @Override
    public String helpTopic() {
        return ht("/Plugin/Wikipedia");
    }

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(dataChangedAdapter, FireMode.IN_EDT_CONSOLIDATED);
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        updateWikipediaArticles();
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(dataChangedAdapter);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
        articles.clear();
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        updateWikipediaArticles();
        list.repaint();
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        final Set<DatasetEventType> typesToProcess = EnumSet.of(
            DatasetEventType.PRIMITIVES_ADDED,
            DatasetEventType.PRIMITIVES_REMOVED,
            DatasetEventType.PRIMITIVE_FLAGS_CHANGED, // Most "delete" commands actually hide the primitive in the dataset.
            DatasetEventType.TAGS_CHANGED);
        final Map<DatasetEventType, List<AbstractDatasetChangedEvent>> events;
        if (event.getType() == DatasetEventType.DATA_CHANGED && event instanceof DataChangedEvent) {
            final Map<DatasetEventType, List<AbstractDatasetChangedEvent>> temporaryEvents =
                getRootEvents((DataChangedEvent) event).collect(Collectors.groupingBy(AbstractDatasetChangedEvent::getType));
            if (temporaryEvents.isEmpty()) {
                events = Collections.singletonMap(event.getType(), Collections.singletonList(event));
            } else {
                events = temporaryEvents;
            }
        } else if (typesToProcess.contains(event.getType())) {
            events = Collections.singletonMap(event.getType(), Collections.singletonList(event));
        } else {
            events = Collections.emptyMap();
        }
        if (events.isEmpty()) {
            return;
        }
        final WikipediaApp app = newWikipediaApp();
        final boolean tagChange = events.getOrDefault(DatasetEventType.TAGS_CHANGED, Collections.emptyList()).stream()
            .filter(TagsChangedEvent.class::isInstance).map(TagsChangedEvent.class::cast)
            .anyMatch(e -> app.tagChangeWikipedia(e.getPrimitive(), e.getOriginalKeys()));
        final boolean primitiveAdded = events.getOrDefault(DatasetEventType.PRIMITIVES_ADDED, Collections.emptyList()).stream()
            .map(AbstractDatasetChangedEvent::getPrimitives).flatMap(Collection::stream).anyMatch(app::hasWikipediaTag);
        final boolean primitiveRemoved = events.getOrDefault(DatasetEventType.PRIMITIVES_REMOVED, Collections.emptyList()).stream()
            .map(AbstractDatasetChangedEvent::getPrimitives).flatMap(Collection::stream).anyMatch(app::hasWikipediaTag);
        final boolean primitiveMaybeRemoved = events.getOrDefault(DatasetEventType.PRIMITIVE_FLAGS_CHANGED, Collections.emptyList()).stream()
            .map(AbstractDatasetChangedEvent::getPrimitives).flatMap(Collection::stream).anyMatch(app::hasWikipediaTag);
        if (!tagChange && !primitiveAdded && !primitiveRemoved && !primitiveMaybeRemoved && !events.containsKey(DatasetEventType.DATA_CHANGED)) {
            return;
        }
        updateWikipediaArticles();
        list.repaint();
    }

    private static Stream<AbstractDatasetChangedEvent> getRootEvents(DataChangedEvent event) {
        if (Utils.isEmpty(event.getEvents())) {
            return Stream.empty();
        }
        return event.getEvents().stream().flatMap(e -> {
            if (e instanceof DataChangedEvent) {
                return getRootEvents(event);
            }
            return Stream.of(e);
        });
    }
}
