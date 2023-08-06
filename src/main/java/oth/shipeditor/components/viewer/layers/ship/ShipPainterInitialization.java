package oth.shipeditor.components.viewer.layers.ship;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.viewer.entities.BoundPoint;
import oth.shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import oth.shipeditor.components.viewer.layers.ship.data.ShipHull;
import oth.shipeditor.components.viewer.painters.points.BoundPointsPainter;
import oth.shipeditor.components.viewer.painters.points.CenterPointPainter;
import oth.shipeditor.components.viewer.painters.points.ShieldPointPainter;
import oth.shipeditor.components.viewer.painters.points.WeaponSlotPainter;
import oth.shipeditor.representation.HullSpecFile;
import oth.shipeditor.representation.ShipData;
import oth.shipeditor.representation.weapon.WeaponMount;
import oth.shipeditor.representation.weapon.WeaponSize;
import oth.shipeditor.representation.weapon.WeaponSlot;
import oth.shipeditor.representation.weapon.WeaponType;
import oth.shipeditor.utility.text.StringConstants;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ontheheavens
 * @since 27.07.2023
 */
@Log4j2
public final class ShipPainterInitialization {

    private ShipPainterInitialization() {
    }

    static void loadShipData(ShipPainter shipPainter, ShipLayer layer) {
        ShipData shipData = layer.getShipData();

        HullSpecFile hullSpecFile = shipData.getHullSpecFile();

        ShipHull shipHull = layer.getHull();
        shipHull.initHullStyle(hullSpecFile);

        Point2D anchor = shipPainter.getCenterAnchor();
        Point2D hullCenter = hullSpecFile.getCenter();

        Point2D translatedCenter = ShipPainterInitialization.rotateHullCenter(hullCenter, anchor);

        ShipPainterInitialization.initCentroids(shipPainter, layer, translatedCenter);

        ShipPainterInitialization.initBounds(shipPainter, hullSpecFile, translatedCenter);

        ShipPainterInitialization.initWeaponSlots(shipPainter, shipData, translatedCenter);

        shipPainter.finishInitialization();
    }

    private static void initCentroids(ShipPainter shipPainter, ShipLayer layer, Point2D translatedCenter) {
        ShipData shipData = layer.getShipData();
        HullSpecFile hullSpecFile = shipData.getHullSpecFile();

        CenterPointPainter centerPointPainter = shipPainter.getCenterPointPainter();
        centerPointPainter.initCenterPoint(translatedCenter, hullSpecFile);

        Point2D shieldCenter = hullSpecFile.getShieldCenter();

        Point2D shieldCenterTranslated = ShipPainterInitialization.rotatePointByCenter(shieldCenter, translatedCenter);
        ShieldPointPainter shieldPointPainter = shipPainter.getShieldPointPainter();
        shieldPointPainter.initShieldPoint(shieldCenterTranslated, layer);
    }

    private static void initBounds(ShipPainter shipPainter, HullSpecFile hullSpecFile, Point2D translatedCenter) {
        Stream<Point2D> boundStream = Arrays.stream(hullSpecFile.getBounds());
        BoundPointsPainter boundsPainter = shipPainter.getBoundsPainter();
        boundStream.forEach(bound -> {
            Point2D rotatedPosition = ShipPainterInitialization.rotatePointByCenter(bound, translatedCenter);
            BoundPoint boundPoint = new BoundPoint(rotatedPosition, shipPainter);
            boundsPainter.addPoint(boundPoint);
        });
    }

    private static void initWeaponSlots(ShipPainter shipPainter, ShipData shipData, Point2D translatedCenter) {
        HullSpecFile hullSpecFile = shipData.getHullSpecFile();
        Stream<WeaponSlot> slotStream = Arrays.stream(hullSpecFile.getWeaponSlots());
        WeaponSlotPainter slotPainter = shipPainter.getWeaponSlotPainter();
        slotStream.forEach(weaponSlot -> {
            if (Objects.equals(weaponSlot.getType(), StringConstants.LAUNCH_BAY)) return;
            Point2D location = weaponSlot.getLocations()[0];
            Point2D rotatedPosition = ShipPainterInitialization.rotatePointByCenter(location, translatedCenter);

            WeaponSlotPoint slotPoint = new WeaponSlotPoint(rotatedPosition, shipPainter);
            slotPoint.setId(weaponSlot.getId());

            slotPoint.setAngle(weaponSlot.getAngle());
            slotPoint.setArc(weaponSlot.getArc());
            slotPoint.setRenderOrderMod((int) weaponSlot.getRenderOrderMod());

            String weaponType = weaponSlot.getType();
            WeaponType typeInstance = WeaponType.valueOf(weaponType);
            slotPoint.setWeaponType(typeInstance);

            String weaponSize = weaponSlot.getSize();
            WeaponSize sizeInstance = WeaponSize.valueOf(weaponSize);
            slotPoint.setWeaponSize(sizeInstance);

            String weaponMount = weaponSlot.getMount();
            WeaponMount mountInstance = WeaponMount.valueOf(weaponMount);
            slotPoint.setWeaponMount(mountInstance);

            slotPainter.addPoint(slotPoint);
        });
    }

    /**
     * Rotates point 90 degrees counterclockwise around specified center point.
     * @param input point to be rotated.
     * @param translatedCenter center point around which the rotation is performed.
     * @return new {@code Point2D} representing the rotated point.
     */
    private static Point2D rotatePointByCenter(Point2D input, Point2D translatedCenter) {
        double translatedX = -input.getY() + translatedCenter.getX();
        double translatedY = -input.getX() + translatedCenter.getY();
        return new Point2D.Double(translatedX, translatedY);
    }

    private static Point2D rotateHullCenter(Point2D hullCenter, Point2D anchor) {
        double anchorX = anchor.getX();
        double anchorY = anchor.getY();
        return new Point2D.Double(hullCenter.getX() + anchorX, -hullCenter.getY() + anchorY);
    }

}
