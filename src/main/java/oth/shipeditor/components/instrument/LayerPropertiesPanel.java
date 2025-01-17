package oth.shipeditor.components.instrument;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.painters.PainterVisibility;
import oth.shipeditor.components.viewer.painters.points.AbstractPointPainter;
import oth.shipeditor.utility.components.ComponentUtilities;
import oth.shipeditor.utility.objects.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class is the preferred way of implementing various property panels like hull/skin data values.
 * Handles refresh and population of values on layer selection.
 * @author Ontheheavens
 * @since 29.10.2023
 */
@Getter
public abstract class LayerPropertiesPanel extends JPanel {

    private final Map<JComponent, Consumer<LayerPainter>> clearingListeners;

    private final Map<JComponent, Consumer<LayerPainter>> refresherListeners;

    private boolean widgetsReadyForInput;

    @Setter
    private LayerPainter cachedLayerPainter;

    protected LayerPropertiesPanel() {
        clearingListeners = new LinkedHashMap<>();
        refresherListeners = new LinkedHashMap<>();
        this.populateContent();
    }

    /**
     * Default method for populating widgets.
     * @return panel with added widgets, which uses GridBagLayout.
     */
    protected JPanel createWidgetsPanel(Map<JLabel, JComponent> widgets) {
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new GridBagLayout());

        int ordering = 0;
        for (Map.Entry<JLabel, JComponent> entry : widgets.entrySet()) {
            JLabel label = entry.getKey();
            JComponent widget = entry.getValue();
            addWidgetRow(contentContainer, label, widget, ordering);
            ordering++;
        }

        return contentContainer;
    }

    public void refresh(LayerPainter layer) {
        cachedLayerPainter = layer;
        widgetsReadyForInput = false;
        this.refreshContent(layer);
        widgetsReadyForInput = true;
    }

    protected void addWidgetRow(JPanel contentContainer, JLabel label, JComponent component, int ordering) {
        ComponentUtilities.addLabelAndComponent(contentContainer, label, component, ordering);
    }

    /**
     * To be called at any significant layer change, like selection. Not expected to add or remove components.
     */
    public abstract void refreshContent(LayerPainter layerPainter);

    /**
     * Called once at panel creation to install all sub-components.
     */
    protected abstract void populateContent();

    protected void processChange() {
        this.refresh(cachedLayerPainter);
    }

    protected void fireClearingListeners(LayerPainter layer) {
        clearingListeners.forEach((widget, clearer) -> clearer.accept(layer));
    }

    protected void fireRefresherListeners(LayerPainter layer) {
        refresherListeners.forEach((widget, refresher) -> refresher.accept(layer));
    }

    protected void registerWidgetListeners(JComponent widget,
                                           Consumer<LayerPainter> clearer,
                                           Consumer<LayerPainter> refresher) {
        registerWidgetClearer(widget, clearer);
        registerWidgetRefresher(widget, refresher);
    }

    protected void registerWidgetClearer(JComponent widget,
                                           Consumer<LayerPainter> clearer) {
        clearingListeners.put(widget, clearer);
    }

    protected void registerWidgetRefresher(JComponent widget,
                                         Consumer<LayerPainter> refresher) {
        refresherListeners.put(widget, refresher);
    }

    protected void clearAllWidgetListeners() {
        clearingListeners.clear();
        refresherListeners.clear();
    }

    protected Pair<JLabel, JComboBox<PainterVisibility>> createVisibilityWidget(Function<LayerPainter,
            AbstractPointPainter> getter) {
        return createVisibilityWidget(getter, null);
    }

    protected Pair<JLabel, JComboBox<PainterVisibility>> createVisibilityWidget(Function<LayerPainter,
            AbstractPointPainter> getter, Consumer<PainterVisibility> additionalAction) {
        BooleanSupplier readinessChecker = this::isWidgetsReadyForInput;
        Consumer<PainterVisibility> visibilitySetter = changedValue -> {
            LayerPainter layerPainter = getCachedLayerPainter();
            if (layerPainter != null) {
                AbstractPointPainter pointPainter = getter.apply(layerPainter);
                if (pointPainter != null) {
                    pointPainter.setVisibilityMode(changedValue);
                    processChange();
                } else {
                    throw new IllegalStateException("Invalid usage of visibility widget!");
                }
            }
            if (additionalAction != null) {
                additionalAction.accept(changedValue);
            }
        };

        BiConsumer<JComponent, Consumer<LayerPainter>> clearerListener = this::registerWidgetClearer;
        BiConsumer<JComponent, Consumer<LayerPainter>> refresherListener = this::registerWidgetRefresher;

        Function<LayerPainter, PainterVisibility> visibilityGetter = layerPainter -> {
            AbstractPointPainter pointPainter = getter.apply(cachedLayerPainter);
            return pointPainter.getVisibilityMode();
        };

        return PainterVisibility.createVisibilityWidget(
                readinessChecker, visibilityGetter,
                visibilitySetter, clearerListener, refresherListener
        );
    }


}
