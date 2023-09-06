package oth.shipeditor.components.viewer.painters.features;

import de.javagl.viewer.Painter;
import lombok.Getter;
import oth.shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.weapon.WeaponPainter;
import oth.shipeditor.components.viewer.painters.points.AbstractPointPainter;
import oth.shipeditor.components.viewer.painters.points.WeaponSlotPainter;
import oth.shipeditor.utility.Utility;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * @author Ontheheavens
 * @since 27.08.2023
 */
public class InstalledSlotFeaturePainter implements Painter {

    @Getter
    private final Map<String, LayerPainter> installedFeatures;

    public InstalledSlotFeaturePainter() {
        installedFeatures = new LinkedHashMap<>();
    }

    public void refreshSlotData(WeaponSlotPainter slotPainter) {
        for (Map.Entry<String, LayerPainter> entry : installedFeatures.entrySet()) {
            String slotID = entry.getKey();
            LayerPainter painter = entry.getValue();
            painter.setShouldDrawPainter(false);

            WeaponSlotPoint slotPoint = slotPainter.getSlotByID(slotID);
            InstalledSlotFeaturePainter.refreshInstalledPainter(slotPoint, painter);
        }
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static void refreshInstalledPainter(WeaponSlotPoint slotPoint, LayerPainter painter) {
        if (slotPoint == null) {
            return;
        }
        painter.setShouldDrawPainter(true);

        Point2D position = slotPoint.getPosition();
        Point2D entityCenter = painter.getEntityCenter();
        if (painter instanceof WeaponPainter weaponPainter) {
            entityCenter = weaponPainter.getWeaponCenter();
        } else if (painter instanceof ShipPainter shipPainter) {
            entityCenter = shipPainter.getCenterAnchorDifference();
        }
        double x = position.getX() - entityCenter.getX();
        double y = position.getY() - entityCenter.getY();
        Point2D newAnchor = new Point2D.Double(x, y);
        Point2D painterAnchor = painter.getAnchor();
        if (!painterAnchor.equals(newAnchor)) {
            painter.setAnchor(newAnchor);
        }

        double transformedAngle = Utility.transformAngle(slotPoint.getAngle());
        painter.setRotationRadians(Math.toRadians(transformedAngle + 90));
    }

    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, double w, double h) {
        Set<Map.Entry<String, LayerPainter>> entries = installedFeatures.entrySet();
        for (Map.Entry<String, LayerPainter> entry : entries) {
            LayerPainter painter = entry.getValue();
            AffineTransform transform = painter.getWithRotation(worldToScreen);
            painter.paint(g, transform, w, h);

            List<AbstractPointPainter> allPainters = painter.getAllPainters();
            allPainters.forEach(pointPainter -> pointPainter.paint(g, transform, w, h));
        }
    }

}
