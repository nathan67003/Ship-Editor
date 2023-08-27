package oth.shipeditor.components.viewer;

import de.javagl.viewer.Viewer;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.ViewerFocusRequestQueued;
import oth.shipeditor.communication.events.viewer.ViewerBackgroundChanged;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.communication.events.viewer.control.ViewerGuidesToggled;
import oth.shipeditor.communication.events.viewer.control.ViewerTransformsReset;
import oth.shipeditor.communication.events.viewer.layers.*;
import oth.shipeditor.components.viewer.control.LayerViewerControls;
import oth.shipeditor.components.viewer.control.ViewerControl;
import oth.shipeditor.components.viewer.layers.LayerManager;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ViewerLayer;
import oth.shipeditor.components.viewer.layers.ship.ShipLayer;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.weapon.WeaponLayer;
import oth.shipeditor.components.viewer.layers.weapon.WeaponPainter;
import oth.shipeditor.menubar.FileUtilities;
import oth.shipeditor.undo.UndoOverseer;
import oth.shipeditor.utility.StaticController;
import oth.shipeditor.utility.graphics.Sprite;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Locale;

/**
 * Despite being a bit of a God-class, this one is justified in its coupling as a conceptual root of the whole app.
 * It is responsible for the foundation of editing workflow - visual display of ships and its point features.
 * @author Ontheheavens
 * @since 29.04.2023
 */
@SuppressWarnings("OverlyCoupledClass")
@Log4j2
public final class PrimaryViewer extends Viewer implements LayerViewer {

    private static final Dimension minimumPanelSize = new Dimension(240, 120);

    @Getter
    private final LayerManager layerManager;

    @Getter
    private PaintOrderController paintOrderController;

    @Getter
    private boolean cursorInViewer;

    public PrimaryViewer() {
        this.setMinimumSize(minimumPanelSize);
        this.setBackground(Color.GRAY);

        this.layerManager = new LayerManager();
        this.layerManager.initListeners();
    }

    public PrimaryViewer commenceInitialization() {
        this.paintOrderController = new PaintOrderController(this);
        this.addPainter(this.paintOrderController);

        EventBus.publish(new ViewerGuidesToggled(true, true,
                true, true));

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                PrimaryViewer.this.requestFocusInWindow();
                cursorInViewer = true;
                setRepaintQueued();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cursorInViewer = false;
                setRepaintQueued();
            }
        });

        ViewerControl controls = LayerViewerControls.create(this);
        this.setMouseControl(controls);
        this.initViewerStateListeners();
        this.initLayerListening();
        this.setDropTarget(new SpriteDropReceiver());
        StaticController.setViewer(this);
        return this;
    }

    public void setRepaintQueued() {
        this.paintOrderController.setRepaintQueued(true);
    }

    @SuppressWarnings("WeakerAccess")
    public void setRevalidateQueued() {
        this.paintOrderController.setRevalidateQueued(true);
    }

    private void initViewerStateListeners() {
        EventBus.subscribe(event -> {
            if(event instanceof ViewerRepaintQueued || event instanceof LayerWasSelected) {
                setRepaintQueued();
            }
        });
        EventBus.subscribe(event -> {
            if(event instanceof ViewerFocusRequestQueued) {
                this.requestFocusInWindow();
            }
        });
        EventBus.subscribe(event -> {
            if(event instanceof ViewerTransformsReset) {
                this.resetTransform();
                this.centerViewpoint();
            }
        });
        EventBus.subscribe(event -> {
            if(event instanceof ViewerBackgroundChanged checked) {
                Color background = checked.newColor();
                Color opaque = new Color(background.getRed(),
                        background.getGreen(), background.getBlue(), 255);
                this.setBackground(opaque);
                setRevalidateQueued();
                setRepaintQueued();
            }
        });
    }

    private void initLayerListening() {
        EventBus.subscribe(event -> {
            if (event instanceof LayerSpriteLoadQueued checked) {
                ViewerLayer newLayer = checked.updated();
                Sprite sprite = checked.sprite();
                if (newLayer.getPainter() == null && sprite != null) {
                    this.loadLayer(newLayer, sprite);
                }
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof ViewerLayerRemovalConfirmed checked) {
                PrimaryViewer.unloadLayer(checked.removed());
                setRepaintQueued();
            }
        });
    }

    @Override
    public AffineTransform getTransformWorldToScreen() {
        return this.getWorldToScreen();
    }

    /**
     * @return layer that is currently active in viewer; might be null, in which case caller is expected to handle that.
     */
    @Override
    @SuppressWarnings("ReturnOfNull")
    public LayerPainter getSelectedLayer() {
        ViewerLayer activeLayer = layerManager.getActiveLayer();
        if (activeLayer == null) {
            return null;
        }
        return activeLayer.getPainter();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    public void loadLayer(ViewerLayer layer, Sprite sprite) {
        // Main sprite painter and said painter children point painters are distinct conceptually.
        // Layer might be selected and deselected, in which case children painters are loaded/unloaded.
        // At the same time main sprite painter remains loaded until layer is explicitly removed.
        LayerPainter newPainter = null;
        if (layer instanceof ShipLayer checkedLayer) {
            ShipPainter shipPainter = new ShipPainter(checkedLayer);
            shipPainter.setBaseHullSprite(sprite);
            newPainter = shipPainter;
        } else if (layer instanceof WeaponLayer checkedLayer) {
            newPainter = new WeaponPainter(checkedLayer);
        }
        layer.setPainter(newPainter);
        layerManager.setActiveLayer(layer);

        if (newPainter != null) {
            newPainter.setSprite(sprite.getSpriteImage());
        }
        layer.setSpriteFileName(sprite.getFileName());
        EventBus.publish(new LayerSpriteLoadConfirmed(layer, sprite));
        EventBus.publish(new ActiveLayerUpdated(layer));
        this.centerViewpoint();
    }

    private static void unloadLayer(ViewerLayer layer) {
        LayerPainter mainPainter = layer.getPainter();
        UndoOverseer.cleanupRemovedLayer(mainPainter);
    }

    public void centerViewpoint() {
        ViewerLayer activeLayer = this.layerManager.getActiveLayer();
        if (activeLayer == null) return;
        AffineTransform worldToScreen = this.getWorldToScreen();
        // Get the center of the sprite in screen coordinates.
        LayerPainter activePainter = activeLayer.getPainter();
        Point2D spriteCenter = activePainter.getSpriteCenter();
        Point2D centerScreen = worldToScreen.transform(spriteCenter, null);
        // Calculate the delta values to center the sprite.
        Point2D midpoint = this.getViewerMidpoint();
        double dx = midpoint.getX() - centerScreen.getX();
        double dy = midpoint.getY() - centerScreen.getY();
        this.translate(dx, dy);
        setRepaintQueued();
    }

    public Point2D getViewerMidpoint() {
        double x = (this.getWidth() / 2.0f);
        double y = (this.getHeight() / 2.0f);
        return new Point2D.Double(x, y);
    }

    private static class SpriteDropReceiver extends DropTarget {
        @SuppressWarnings({"unchecked", "AccessToStaticFieldLockedOnInstance"})
        public synchronized void drop(DropTargetDropEvent dtde) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = dtde.getTransferable();
                Iterable<File> droppedFiles = (Iterable<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                File firstEligible = null;
                for (File file : droppedFiles) {
                    if (file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
                        firstEligible = file;
                        break;
                    }
                }
                if (firstEligible == null) {
                    log.error("Drag-and-drop sprite loading unsuccessful: wrong file extension.");
                    JOptionPane.showMessageDialog(null,
                            "Failed to load file as sprite or initialize layer with it: invalid file extension.",
                            "Drag-and-drop layer initialization unsuccessful!",
                            JOptionPane.INFORMATION_MESSAGE);
                    dtde.dropComplete(false);
                    return;
                }
                FileUtilities.createShipLayerWithSprite(firstEligible);
                dtde.dropComplete(true);
            } catch (Exception ex) {
                dtde.dropComplete(false);
                log.error("Drag-and-drop sprite loading failed!");
                JOptionPane.showMessageDialog(null,
                        "Failed to load file as sprite or initialize layer with it, exception thrown at: " + dtde,
                        "Drag-and-drop layer initialization error!",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

    }

}