package oth.shipeditor.components.viewer.control;

import de.javagl.viewer.InputEventPredicates;
import de.javagl.viewer.Predicates;
import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.viewer.control.*;

import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * @author Ontheheavens
 * @since 01.06.2023
 */
@SuppressWarnings("ClassWithTooManyFields")
public final class ControlPredicates {

    public static final double MAXIMUM_ZOOM = 1000.0;
    public static final double MINIMUM_ZOOM = 0.1;

    public static final double ZOOMING_SPEED = 1.25;
    @SuppressWarnings("WeakerAccess")
    public static final double ROTATION_SPEED = 6.0;
    @Getter
    private static PointSelectionMode selectionMode = PointSelectionMode.CLOSEST;

    @Getter
    private static boolean mirrorModeEnabled = true;

    @Getter
    private static boolean cursorSnappingEnabled = true;

    @Getter
    private static boolean rotationRoundingEnabled = true;

    @Getter @Setter
    private static boolean selectionHoldingEnabled = true;

    @Getter
    private static int mirrorPointLinkageTolerance;

    public static void initSelectionModeListening() {
        EventBus.subscribe(event -> {
            if (event instanceof PointSelectionModeChange checked) {
                selectionMode = checked.newMode();
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof MirrorModeChange checked) {
                mirrorModeEnabled = checked.enabled();
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof CursorSnappingToggled checked) {
                cursorSnappingEnabled = checked.toggled();
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof RotationRoundingToggled checked) {
                rotationRoundingEnabled = checked.toggled();
            }
        });
        EventBus.subscribe(event -> {
            if (event instanceof PointLinkageToleranceChanged checked) {
                mirrorPointLinkageTolerance = checked.changed();
            }
        });
    }

    static final Predicate<MouseEvent> translatePredicate = Predicates.and(
            InputEventPredicates.buttonDown(2),
            InputEventPredicates.noModifiers()
    );

    static final Predicate<MouseEvent> layerMovePredicate = Predicates.and(
            InputEventPredicates.buttonDown(1),
            InputEventPredicates.shiftDown()
    );

    static final Predicate<MouseEvent> layerRotatePredicate = Predicates.and(
            InputEventPredicates.buttonDown(3),
            InputEventPredicates.shiftDown()
    );

    static final Predicate<MouseEvent> removePointPredicate = Predicates.and(
            InputEventPredicates.buttonDown(3),
            InputEventPredicates.controlDown()
    );

    static final Predicate<MouseEvent> selectPointPredicate = Predicates.and(
            InputEventPredicates.buttonDown(1),
            InputEventPredicates.noModifiers()
    );

    static final Predicate<MouseEvent> changeAnglePredicate = Predicates.and(
            InputEventPredicates.buttonDown(1),
            InputEventPredicates.noModifiers()
    );

    static final Predicate<MouseEvent> changeArcOrSizePredicate = Predicates.and(
            InputEventPredicates.buttonDown(3),
            InputEventPredicates.noModifiers()
    );

    static final Predicate<MouseEvent> rotatePredicate = InputEventPredicates.controlDown();

    private ControlPredicates() {
    }

}
