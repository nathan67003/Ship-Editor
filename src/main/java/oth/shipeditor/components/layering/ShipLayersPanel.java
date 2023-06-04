package oth.shipeditor.components.layering;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.WindowRepaintQueued;
import oth.shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import oth.shipeditor.communication.events.viewer.layers.ShipLayerCreated;
import oth.shipeditor.communication.events.viewer.layers.ShipLayerRemovalConfirmed;
import oth.shipeditor.components.viewer.layers.LayerManager;
import oth.shipeditor.components.viewer.layers.ShipLayer;
import oth.shipeditor.representation.ShipData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ontheheavens
 * @since 01.06.2023
 */
@Log4j2
public final class ShipLayersPanel extends JTabbedPane {

    // TODO: Consider implementing custom JTabbedPane UI in the future to allow for custom panel compositions.

    /**
     * Expected to be the same instance that is originally created and assigned in viewer;
     * Reference in this class is present for both conceptual and convenience purposes.
     */
    private final LayerManager layerManager;

    private final Map<ShipLayer, LayerTab> tabIndex;

    public ShipLayersPanel(LayerManager manager) {
        this.layerManager = manager;
        this.tabIndex = new HashMap<>();
        this.initLayerListeners();
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.addChangeListener(event -> {
            ShipLayer newlySelected = getLayerByTab((LayerTab) getSelectedComponent());
            log.info("Layer panel change!");
            // If the change results from the last layer being removed and the newly selected layer is null,
            // call to set active layer is unnecessary as this case is handled directly by layer manager.
            if (newlySelected != null) {
                layerManager.setActiveLayer(newlySelected);
            }
        });
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
    }

    private void initLayerListeners() {
        EventBus.subscribe(event -> {
            if (event instanceof ShipLayerCreated checked) {
                Icon tabIcon = FontIcon.of(FluentUiRegularMZ.ROCKET_20, 20);
                ShipLayer layer = checked.newLayer();
                LayerTab created = new LayerTab(layer);
                tabIndex.put(layer, created);
                String tooltip = created.getTabTooltip();
                this.addTab("Layer #" + getTabCount(), tabIcon, tabIndex.get(layer), tooltip);
                EventBus.publish(new WindowRepaintQueued());
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof ActiveLayerUpdated checked) {
                ShipLayer eventLayer = checked.updated();
                LayerTab updated = tabIndex.get(eventLayer);
                BufferedImage sprite = eventLayer.getShipSprite();
                ShipData hullFile = eventLayer.getShipData();
                ShipLayer associatedLayer = updated.getAssociatedLayer();
                if (sprite != null) {
                    updated.setSpriteFileName(associatedLayer.getSpriteFileName());
                    this.setToolTipTextAt(indexOfComponent(updated), updated.getTabTooltip());
                }
                if (hullFile != null) {
                    updated.setHullFileName(associatedLayer.getHullFileName());
                    this.setToolTipTextAt(indexOfComponent(updated), updated.getTabTooltip());
                }
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof ShipLayerRemovalConfirmed checked) {
                ShipLayer layer = checked.removed();
                this.removeTabAt(indexOfComponent(tabIndex.get(layer)));
                tabIndex.remove(layer);
                EventBus.publish(new WindowRepaintQueued());
            }
        });
    }

    /**
     * Empty marker component, only serves to track tabs and their layers.
     */
    private static final class LayerTab extends JPanel {

        @Getter
        private final ShipLayer associatedLayer;

        @Getter @Setter
        private String spriteFileName;

        @Getter @Setter
        private String hullFileName;

        private LayerTab(ShipLayer layer) {
            this.associatedLayer = layer;
            this.spriteFileName = layer.getSpriteFileName();
            this.hullFileName = layer.getHullFileName();
            this.setLayout(new BorderLayout());
        }

        private String getTabTooltip() {
            String notLoaded = "Not loaded";
            String sprite = spriteFileName;
            if (Objects.equals(sprite, "")) {
                sprite = notLoaded;
            }
            String spriteNameLine = "Sprite file: " + sprite;
            String hull = hullFileName;
            if (Objects.equals(hull, "")) {
                hull = notLoaded;
            }
            String hullNameLine = "Hull file: " + hull;
            return "<html>" + spriteNameLine + "<br>" + hullNameLine + "</html>";
        }

    }

    private ShipLayer getLayerByTab(LayerTab value) {
        ShipLayer result;
        for (Map.Entry<ShipLayer, LayerTab> entry : tabIndex.entrySet()) {
            LayerTab entryValue = entry.getValue();
            if (entryValue.equals(value)) {
                result = entry.getKey();
                return result;
            }
        }
        return null;
    }

}