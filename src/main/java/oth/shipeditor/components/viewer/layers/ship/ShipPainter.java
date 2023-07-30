package oth.shipeditor.components.viewer.layers.ship;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.BusEventListener;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.Events;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import oth.shipeditor.communication.events.viewer.layers.LayerSpriteLoadQueued;
import oth.shipeditor.communication.events.viewer.layers.ships.LayerShipDataInitialized;
import oth.shipeditor.communication.events.viewer.layers.ships.ShipDataCreated;
import oth.shipeditor.components.viewer.entities.ShipCenterPoint;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.painters.points.*;
import oth.shipeditor.representation.ShipData;
import oth.shipeditor.representation.Skin;
import oth.shipeditor.utility.graphics.Sprite;
import oth.shipeditor.utility.text.StringValues;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Distinct from parent ship layer instance: present class has to do with direct visual representation.
 * Painter instance is not concerned with loading and file interactions, and leaves that to other classes.
 * @author Ontheheavens
 * @since 29.05.2023
 */
@SuppressWarnings("OverlyCoupledClass")
@Log4j2
public final class ShipPainter extends LayerPainter {

    @Getter
    private BoundPointsPainter boundsPainter;
    @Getter
    private CenterPointPainter centerPointPainter;

    @Getter
    private ShieldPointPainter shieldPointPainter;

    @Getter
    private WeaponSlotPainter weaponSlotPainter;

    /**
     * Backup for when sprite is switched to skin version.
     */
    @Getter @Setter
    private Sprite baseHullSprite;

    @Getter @Setter
    private Skin activeSkin;

    public ShipPainter(ShipLayer layer) {
        super(layer);
        this.initPainterListeners(layer);
    }

    /**
     * @param skinID only evaluated if spec type is SKIN.
     */
    @SuppressWarnings("unused")
    public void setActiveSpec(ActiveShipSpec type, String skinID) {
        ShipLayer parentLayer = this.getParentLayer();
        if (type == ActiveShipSpec.HULL) {
            this.setSprite(baseHullSprite.getSpriteImage());
            parentLayer.setSpriteFileName(baseHullSprite.getFileName());
            parentLayer.setSkinFileName(StringValues.NOT_LOADED);
        } else {
            ShipLayer shipLayer = getParentLayer();
            ShipData shipData = shipLayer.getShipData();
            Map<String, Skin> skins = shipData.getSkins();
            Skin retrieved = skins.get(skinID);
            Sprite skinSprite = retrieved.getLoadedSkinSprite();
            this.setSprite(skinSprite.getSpriteImage());
            parentLayer.setSpriteFileName(skinSprite.getFileName());
            String skinFileName = retrieved.getSkinFilePath().getFileName().toString();
            parentLayer.setSkinFileName(skinFileName);
        }
        EventBus.publish(new ActiveLayerUpdated(this.getParentLayer()));
        Events.repaintView();
    }

    @Override
    public ShipLayer getParentLayer() {
        if (super.getParentLayer() instanceof ShipLayer checked) {
            return checked;
        } else throw new IllegalStateException("Found illegal parent layer of ShipPainter!");
    }

    private void createPointPainters() {
        this.centerPointPainter = new CenterPointPainter(this);
        this.shieldPointPainter = new ShieldPointPainter(this);
        this.boundsPainter = new BoundPointsPainter(this);
        this.weaponSlotPainter = new WeaponSlotPainter(this);

        List<AbstractPointPainter> allPainters = getAllPainters();
        allPainters.add(centerPointPainter);
        allPainters.add(shieldPointPainter);
        allPainters.add(boundsPainter);
        allPainters.add(weaponSlotPainter);
    }
    void finishInitialization() {
        this.setUninitialized(false);
        log.info("{} initialized!", this);
        EventBus.publish(new LayerShipDataInitialized(this));
        EventBus.publish(new ViewerRepaintQueued());
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private void initPainterListeners(ShipLayer layer) {
        BusEventListener layerUpdateListener = event -> {
            if (event instanceof LayerSpriteLoadQueued checked) {
                if (checked.updated() != layer) return;
                if (layer.getSprite() != null) {
                    this.setSprite(layer.getSprite());
                }
            } else if (event instanceof ShipDataCreated checked) {
                if (checked.layer() != layer) return;
                if (layer.getShipData() != null && this.isUninitialized()) {
                    this.createPointPainters();
                    ShipPainterInitialization.loadShipData(this, layer.getShipData());
                }
            }
        };
        List<BusEventListener> listeners = getListeners();
        listeners.add(layerUpdateListener);
        EventBus.subscribe(layerUpdateListener);
    }



    public ShipCenterPoint getShipCenter() {
        return this.centerPointPainter.getCenterPoint();
    }

    public Point2D getCenterAnchor() {
        Point2D anchor = getAnchor();
        BufferedImage sprite = getSprite();
        return new Point2D.Double( anchor.getX(), anchor.getY() + sprite.getHeight());
    }

}
