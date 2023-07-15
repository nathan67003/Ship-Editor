package oth.shipeditor.components.viewer.painters;

import de.javagl.viewer.Painter;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.Events;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.communication.events.viewer.points.BoundCreationQueued;
import oth.shipeditor.communication.events.viewer.points.BoundInsertedConfirmed;
import oth.shipeditor.communication.events.viewer.points.InstrumentModeChanged;
import oth.shipeditor.communication.events.viewer.points.PointSelectedConfirmed;
import oth.shipeditor.components.instrument.InstrumentTabsPane;
import oth.shipeditor.components.viewer.InstrumentMode;
import oth.shipeditor.components.viewer.PrimaryShipViewer;
import oth.shipeditor.components.viewer.control.ControlPredicates;
import oth.shipeditor.components.viewer.control.PointSelectionMode;
import oth.shipeditor.components.viewer.control.ViewerControl;
import oth.shipeditor.components.viewer.entities.BaseWorldPoint;
import oth.shipeditor.components.viewer.entities.BoundPoint;
import oth.shipeditor.components.viewer.entities.ShipCenterPoint;
import oth.shipeditor.components.viewer.entities.WorldPoint;
import oth.shipeditor.components.viewer.layers.LayerPainter;
import oth.shipeditor.undo.EditDispatch;
import oth.shipeditor.utility.Utility;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ontheheavens
 * @since 06.05.2023
 */
@Log4j2
public final class BoundPointsPainter extends AbstractPointPainter {

    // TODO: implement always-show-points checkbox toggle and separate interaction access check.
    //  Should be unable to interact with bounds unless bound tab is active.

    @Getter
    private final List<BoundPoint> boundPoints;

    private boolean appendBoundHotkeyPressed;
    private boolean insertBoundHotkeyPressed;

    private final PrimaryShipViewer viewerPanel;

    private final LayerPainter parentLayer;

    private final int appendBoundHotkey = KeyEvent.VK_Z;
    private final int insertBoundHotkey = KeyEvent.VK_X;

    public BoundPointsPainter(PrimaryShipViewer viewer, LayerPainter associatedLayer) {
        this.viewerPanel = viewer;
        this.parentLayer = associatedLayer;
        this.boundPoints = new ArrayList<>();
        this.initHotkeys();
        this.initModeListener();
        this.initCreationListener();
        this.setInteractionEnabled(InstrumentTabsPane.getCurrentMode() == InstrumentMode.BOUNDS);
    }

    @Override
    public boolean isInteractionEnabled() {
        return super.isInteractionEnabled() && this.parentLayer.isLayerActive();
    }

    @Override
    public boolean isMirrorable() {
        return true;
    }

    @Override
    protected void selectPointConditionally() {
        PointSelectionMode current = ControlPredicates.getSelectionMode();
        if (current == PointSelectionMode.STRICT) {
            super.selectPointConditionally();
            return;
        }
        Point2D correctedCursor = AbstractPointPainter.getCorrectedCursor();
        WorldPoint toSelect = null;
        double minDistance = Double.MAX_VALUE;
        for (BoundPoint point : this.boundPoints) {
            Point2D position = point.getPosition();
            double distance = position.distance(correctedCursor);
            if (distance < minDistance) {
                minDistance = distance;
                toSelect = point;
            }
        }
        WorldPoint selected = this.getSelected();
        if (selected != null) {
            selected.setSelected(false);
        }
        this.setSelected(toSelect);
        if (toSelect != null) {
            toSelect.setSelected(true);
        }
        EventBus.publish(new PointSelectedConfirmed(toSelect));
        EventBus.publish(new ViewerRepaintQueued());
    }

    @Override
    public BaseWorldPoint getMirroredCounterpart(WorldPoint point) {
        List<BoundPoint> bounds = this.getBoundPoints();
        Point2D pointPosition = point.getPosition();
        Point2D counterpartPosition = this.createCounterpartPosition(pointPosition);
        double threshold = 2.0d; // Adjust the threshold value as needed; default 2 scaled pixels.
        BoundPoint closestBound = null;
        double closestDistance = Double.MAX_VALUE;
        for (BoundPoint bound : bounds) {
            double distance = counterpartPosition.distance(bound.getPosition());
            if (distance < closestDistance) {
                closestBound = bound;
                closestDistance = distance;
            }
        }
        if (closestDistance <= threshold) {
            return closestBound; // Found the mirrored counterpart within the threshold.
        } else {
            return null; // Mirrored counterpart not found.
        }
    }

    @Override
    protected Point2D createCounterpartPosition(Point2D toMirror) {
        ShipCenterPoint shipCenter = parentLayer.getShipCenter();
        Point2D centerPosition = shipCenter.getPosition();
        double counterpartX = 2 * centerPosition.getX() - toMirror.getX();
        double counterpartY = toMirror.getY(); // Y-coordinate remains the same.
        return new Point2D.Double(counterpartX, counterpartY);
    }

    @Override
    protected BoundPoint getTypeReference() {
        return new BoundPoint(new Point2D.Double());
    }

    @Override
    protected List<BoundPoint> getPointsIndex() {
        return boundPoints;
    }

    @Override
    protected void addPointToIndex(BaseWorldPoint point) {
        if (point instanceof BoundPoint checked) {
            boundPoints.add(checked);
        } else {
            throw new IllegalArgumentException("Attempted to add incompatible point to BoundPointsPainter!");
        }
    }

    @Override
    protected void removePointFromIndex(BaseWorldPoint point) {
        if (point instanceof BoundPoint checked) {
            boundPoints.remove(checked);
        } else {
            throw new IllegalArgumentException("Attempted to remove incompatible point from BoundPointsPainter!");
        }
    }

    @Override
    public int getIndexOfPoint(BaseWorldPoint point) {
        if (point instanceof BoundPoint checked) {
            return boundPoints.indexOf(checked);
        } else {
            throw new IllegalArgumentException("Attempted to access incompatible point in BoundPointsPainter!");
        }
    }

    private void initHotkeys() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ke -> {
            int keyCode = ke.getKeyCode();
            // Remember, single equals is assignments, while double is boolean evaluation.
            // First we evaluate whether the passed keycode is one of our hotkeys, then assign the result to field.
            boolean isAppendHotkey = (keyCode == appendBoundHotkey);
            boolean isInsertHotkey = (keyCode == insertBoundHotkey);
            switch (ke.getID()) {
                case KeyEvent.KEY_PRESSED:
                    if (isAppendHotkey || isInsertHotkey) {
                        setHotkeyState(isAppendHotkey, true);
                    }
                    break;
                case KeyEvent.KEY_RELEASED:
                    if (isAppendHotkey || isInsertHotkey) {
                        setHotkeyState(isAppendHotkey, false);
                    }
                    break;
            }
            Events.repaintView();
            return false;
        });
    }

    private void initModeListener() {
        EventBus.subscribe(event -> {
            if (event instanceof InstrumentModeChanged checked) {
                setInteractionEnabled(checked.newMode() == InstrumentMode.BOUNDS);
            }
        });
    }

    private void initCreationListener() {
        EventBus.subscribe(event -> {
            if (event instanceof BoundCreationQueued checked) {
                if (!isInteractionEnabled()) return;
                if (!hasPointAtCoords(checked.position())) {
                    createBound(checked);
                }
            }
        });
    }

    private void createBound(BoundCreationQueued event) {
        Point2D position = event.position();
        boolean mirrorMode = ControlPredicates.isMirrorModeEnabled();
        if (insertBoundHotkeyPressed) {
            if (boundPoints.size() >= 2) {
                int precedingIndex = calculateInsertionIndex(position);
                BoundPoint wrapped = new BoundPoint(position, this.parentLayer);
                EditDispatch.postPointInserted(this, wrapped, precedingIndex);
                log.info(precedingIndex);
                if (mirrorMode) {
                    if (getMirroredCounterpart(wrapped) == null) {
                        Point2D counterpartPosition = createCounterpartPosition(position);
                        int precedingCounterpartIndex = calculateInsertionIndex(counterpartPosition);
                        if (precedingCounterpartIndex == precedingIndex && boundPoints.size() > 3) {
                            precedingCounterpartIndex -= 1;
                        }
                        log.info(precedingCounterpartIndex);
                        BoundPoint wrappedCounterpart = new BoundPoint(counterpartPosition, this.parentLayer);
                        EditDispatch.postPointInserted(this, wrappedCounterpart, precedingCounterpartIndex);
                    }
                }
            }
        } else if (appendBoundHotkeyPressed) {
            BoundPoint wrapped = new BoundPoint(position, this.parentLayer);
            EditDispatch.postPointAdded(this, wrapped);
        }
    }

    private int calculateInsertionIndex(Point2D position) {
        List<BoundPoint> twoClosest = findClosestBoundPoints(position);
        int index = getLowestBoundPointIndex(twoClosest);
        if (index >= 0) index += 1;
        if (index > boundPoints.size() - 1) index = 0;
        if (getHighestBoundPointIndex(twoClosest) == boundPoints.size() - 1 &&
                getLowestBoundPointIndex(twoClosest) == 0) {
            index = 0;
        }
        BoundPoint preceding = boundPoints.get(index);
        return boundPoints.indexOf(preceding);
    }

    @SuppressWarnings("unused")
    public void insertPoint(BoundPoint toInsert, BoundPoint preceding) {
        int precedingIndex = boundPoints.indexOf(preceding);
        this.insertPoint(toInsert, precedingIndex);
    }

    public void insertPoint(BoundPoint toInsert, int precedingIndex) {
        boundPoints.add(precedingIndex, toInsert);
        EventBus.publish(new BoundInsertedConfirmed(toInsert, precedingIndex));
        List<Painter> painters = getDelegates();
        painters.add(toInsert.getPainter());
        log.info("Bound inserted to painter: {}", toInsert);
    }

    private void setHotkeyState(boolean isAppendHotkey, boolean state) {
        if (isAppendHotkey) {
            appendBoundHotkeyPressed = state;
        } else {
            insertBoundHotkeyPressed = state;
        }
    }

    private List<BoundPoint> findClosestBoundPoints(Point2D cursor) {
        List<BoundPoint> pointList = new ArrayList<>(this.boundPoints);
        pointList.add(pointList.get(0)); // Add first point to end of list.
        BoundPoint closestPoint1 = pointList.get(0);
        BoundPoint closestPoint2 = pointList.get(1);
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < pointList.size() - 1; i++) {
            BoundPoint currentPoint = pointList.get(i);
            BoundPoint nextPoint = pointList.get(i+1);
            Line2D segment = new Line2D.Double(currentPoint.getPosition(), nextPoint.getPosition());
            double dist = segment.ptSegDist(cursor);
            if (dist < minDist) {
                closestPoint1 = currentPoint;
                closestPoint2 = nextPoint;
                minDist = dist;
            }
        }
        List<BoundPoint> closestPoints = new ArrayList<>(2);
        closestPoints.add(closestPoint1);
        closestPoints.add(closestPoint2);
        return closestPoints;
    }

    private int getLowestBoundPointIndex(List<BoundPoint> closestPoints) {
        List<BoundPoint> points = boundPoints;
        int index1 = points.indexOf(closestPoints.get(0));
        int index2 = points.indexOf(closestPoints.get(1));
        return Math.min(index1, index2);
    }

    private int getHighestBoundPointIndex(List<BoundPoint> closestPoints) {
        List<BoundPoint> points = this.boundPoints;
        int index1 = points.indexOf(closestPoints.get(0));
        int index2 = points.indexOf(closestPoints.get(1));
        return Math.max(index1, index2);
    }

    @Override
    public void paint(Graphics2D g, AffineTransform worldToScreen, double w, double h) {
        if (!isShown()) return;
        Stroke origStroke = g.getStroke();
        Paint origPaint = g.getPaint();
        List<BoundPoint> bPoints = this.boundPoints;
        if (bPoints.isEmpty()) {
            this.paintIfBoundsEmpty(g, worldToScreen);
            g.setStroke(origStroke);
            g.setPaint(origPaint);
            return;
        }
        drawSelectionHighlight(g, worldToScreen);
        BoundPoint boundPoint = bPoints.get(bPoints.size() - 1);
        Point2D prev = worldToScreen.transform(boundPoint.getPosition(), null);
        for (BoundPoint p : bPoints) {
            Point2D curr = worldToScreen.transform(p.getPosition(), null);
            Utility.drawBorderedLine(g, prev, curr, Color.LIGHT_GRAY);
            prev = curr;
        }
        // Set the color to white for visual convenience.
        BoundPoint anotherBoundPoint = bPoints.get(0);
        Point2D first = worldToScreen.transform(anotherBoundPoint.getPosition(), null);
        Utility.drawBorderedLine(g, prev, first, Color.GREEN);
        boolean hotkeyPressed = appendBoundHotkeyPressed || insertBoundHotkeyPressed;
        if (isInteractionEnabled() && hotkeyPressed) {
            this.paintCreationGuidelines(g, worldToScreen, prev, first);
        }
        g.setStroke(origStroke);
        g.setPaint(origPaint);
        super.paintDelegates(g, worldToScreen, w, h);
    }

    private void drawSelectionHighlight(Graphics2D g, AffineTransform worldToScreen) {
        WorldPoint selection = this.getSelected();
        if (selection != null) {
            BoundPointsPainter.paintPointDot(g, worldToScreen, selection.getPosition(), 2.0f);
            WorldPoint counterpart = this.getMirroredCounterpart(selection);
            if (counterpart != null) {
                BoundPointsPainter.paintPointDot(g, worldToScreen, counterpart.getPosition(), 2.0f);
            }
        }
    }

    private void paintIfBoundsEmpty(Graphics2D g, AffineTransform worldToScreen) {
        ViewerControl viewerControl = viewerPanel.getControls();
        Point2D cursor = viewerControl.getAdjustedCursor();
        AffineTransform screenToWorld = viewerPanel.getScreenToWorld();
        Point2D adjustedWorldCursor = Utility.correctAdjustedCursor(cursor, screenToWorld);
        Point2D worldCounterpart = this.createCounterpartPosition(adjustedWorldCursor);
        boolean hotkeyPressed = appendBoundHotkeyPressed || insertBoundHotkeyPressed;
        if (!isInteractionEnabled() || !hotkeyPressed) return;
        BoundPointsPainter.paintPointDot(g, worldToScreen, adjustedWorldCursor, 1.0f);
        if (ControlPredicates.isMirrorModeEnabled()) {
            Point2D adjustedScreenCursor = worldToScreen.transform(adjustedWorldCursor, null);
            Point2D adjustedScreenCounterpart = worldToScreen.transform(worldCounterpart, null);
            BoundPointsPainter.paintPointDot(g, worldToScreen, worldCounterpart, 1.0f);
            Utility.drawBorderedLine(g, adjustedScreenCursor, adjustedScreenCounterpart, Color.WHITE);
        }
    }


    private void paintCreationGuidelines(Graphics2D g, AffineTransform worldToScreen,
                                         Point2D prev, Point2D first) {
        ViewerControl viewerControl = viewerPanel.getControls();
        Point2D cursor = viewerControl.getAdjustedCursor();
        AffineTransform screenToWorld = viewerPanel.getScreenToWorld();
        Point2D adjustedWorldCursor = Utility.correctAdjustedCursor(cursor, screenToWorld);
        Point2D adjustedScreenCursor = worldToScreen.transform(adjustedWorldCursor, null);
        Point2D worldCounterpart = this.createCounterpartPosition(adjustedWorldCursor);
        Point2D adjustedScreenCounterpart = worldToScreen.transform(worldCounterpart, null);
        boolean mirrorMode = ControlPredicates.isMirrorModeEnabled();
        if (appendBoundHotkeyPressed) {
            if (mirrorMode) {
                Utility.drawBorderedLine(g, prev, adjustedScreenCursor, Color.WHITE);
                Utility.drawBorderedLine(g, adjustedScreenCursor, adjustedScreenCounterpart, Color.WHITE);
                Utility.drawBorderedLine(g, adjustedScreenCounterpart, first, Color.WHITE);
            } else {
                BoundPointsPainter.drawGuidelines(g, prev, first, adjustedScreenCursor);
            }
        } else if (insertBoundHotkeyPressed) {
            this.handleInsertionGuides(g, worldToScreen,
                    adjustedWorldCursor, worldCounterpart);
        }
        // Also paint dots where the points would be placed.
        BoundPointsPainter.paintPointDot(g, worldToScreen, adjustedWorldCursor, 1.0f);
        if (mirrorMode) {
            BoundPointsPainter.paintPointDot(g, worldToScreen, worldCounterpart, 1.0f);
        }
    }

    private static void paintPointDot(Graphics2D g, AffineTransform worldToScreen,
                                      Point2D point, float radiusMult) {
        Color originalColor = g.getColor();
        g.setColor(Color.WHITE);
        Shape worldDot = Utility.createCircle(point, 0.25f * radiusMult);
        Shape screenDot = worldToScreen.createTransformedShape(worldDot);
        Point2D screenPoint = worldToScreen.transform(point, null);
        RectangularShape screenOuterDot = Utility.createCircle(screenPoint, 6.0f * radiusMult);
        int x = (int) screenOuterDot.getX();
        int y = (int) screenOuterDot.getY();
        int width = (int) screenOuterDot.getWidth();
        int height = (int) screenOuterDot.getHeight();
        g.fill(screenDot);
        g.drawOval(x, y, width, height);
        g.setColor(originalColor);
    }

    @SuppressWarnings("DuplicatedCode")
    private void handleInsertionGuides(Graphics2D g, AffineTransform worldToScreen,
                                       Point2D adjustedWorldCursor, Point2D worldCounterpart) {
        List<BoundPoint> closest = this.findClosestBoundPoints(adjustedWorldCursor);
        BoundPoint precedingPoint = closest.get(1);
        Point2D preceding = worldToScreen.transform(precedingPoint.getPosition(), null);
        BoundPoint subsequentPoint = closest.get(0);
        Point2D subsequent = worldToScreen.transform(subsequentPoint.getPosition(), null);
        Point2D transformed = worldToScreen.transform(adjustedWorldCursor, null);

        List<BoundPoint> closestToCounterpart = this.findClosestBoundPoints(worldCounterpart);
        BoundPoint precedingToCounterpart = closestToCounterpart.get(1);
        Point2D precedingTC = worldToScreen.transform(precedingToCounterpart.getPosition(), null);
        BoundPoint subsequentToCounterpart = closestToCounterpart.get(0);
        Point2D subsequentTC = worldToScreen.transform(subsequentToCounterpart.getPosition(), null);
        Point2D transformedCounterpart = worldToScreen.transform(worldCounterpart, null);

        boolean crossingEmerged = preceding.equals(precedingTC) || subsequent.equals(subsequentTC);

        if (ControlPredicates.isMirrorModeEnabled()) {
            if (crossingEmerged) {
                Utility.drawBorderedLine(g, preceding, transformed, Color.WHITE);
                Utility.drawBorderedLine(g, transformed, transformedCounterpart, Color.WHITE);
                Utility.drawBorderedLine(g, transformedCounterpart, subsequent, Color.WHITE);
            } else {
                BoundPointsPainter.drawGuidelines(g, preceding, subsequent, transformed);
                BoundPointsPainter.drawGuidelines(g, precedingTC, subsequentTC, transformedCounterpart);
            }
        } else {
            BoundPointsPainter.drawGuidelines(g, preceding, subsequent, transformed);
        }
    }

    private static void drawGuidelines(Graphics2D g, Point2D preceding, Point2D subsequent, Point2D cursor) {
        Utility.drawBorderedLine(g, preceding, cursor, Color.WHITE);
        Utility.drawBorderedLine(g, subsequent, cursor, Color.WHITE);
    }

}
