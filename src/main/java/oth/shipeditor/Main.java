package oth.shipeditor;

import com.formdev.flatlaf.FlatIntelliJLaf;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.layers.LayerCreationQueued;
import oth.shipeditor.components.viewer.ShipViewable;
import oth.shipeditor.components.viewer.layers.LayerManager;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ShipLayer;
import oth.shipeditor.menubar.Files;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * @author Ontheheavens
 * @since 08.05.2023
 */
@Log4j2
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main.configureLaf();
            PrimaryWindow window = PrimaryWindow.create();
            window.showGUI();
            Main.testFiles(window);
        });
    }

    private static void configureLaf() {
        FlatIntelliJLaf.setup();
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
    }

    private static void testFiles(PrimaryWindow window) {
        String legionSprite = "legion_xiv.png";
        String crigSprite = "salvage_rig.png";
        String legionHull = "legion.ship";
        String crigHull = "constructionrig.ship";
        Main.loadShip(window, crigSprite, crigHull);
        Main.loadShip(window, legionSprite, legionHull);
        ShipViewable shipView = window.getShipView();
        LayerManager layerManager = shipView.getLayerManager();
        ShipLayer activeLayer = layerManager.getActiveLayer();
        LayerPainter painter = activeLayer.getPainter();
        painter.updateAnchorOffset(new Point2D.Double(-400, 0));
        shipView.centerViewpoint();
    }

    private static void loadShip(PrimaryWindow window, String spriteFile, String hullFile) {
        EventBus.publish(new LayerCreationQueued());
        ShipViewable shipView = window.getShipView();
        LayerManager layerManager = shipView.getLayerManager();
        layerManager.activateNextLayer();
        Class<? extends PrimaryWindow> windowClass = window.getClass();
        ClassLoader classLoader = windowClass.getClassLoader();
        URL spritePath = Objects.requireNonNull(classLoader.getResource(spriteFile));
        File sprite;
        try {
            sprite = new File(spritePath.toURI());
            Files.loadSprite(sprite);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        URL dataPath = Objects.requireNonNull(classLoader.getResource(hullFile));;
        try {
            URI url = dataPath.toURI();
            File hull = new File(url);
            Files.loadHullFile(hull);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
