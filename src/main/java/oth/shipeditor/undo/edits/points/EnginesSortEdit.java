package oth.shipeditor.undo.edits.points;

import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.EnginesPanelRepaintQueued;
import oth.shipeditor.communication.events.viewer.ViewerRepaintQueued;
import oth.shipeditor.components.viewer.entities.engine.EnginePoint;
import oth.shipeditor.components.viewer.painters.points.EngineSlotPainter;
import oth.shipeditor.undo.AbstractEdit;

import java.util.List;

/**
 * @author Ontheheavens
 * @since 20.08.2023
 */
public class EnginesSortEdit extends AbstractEdit {

    private final EngineSlotPainter pointPainter;

    private final List<EnginePoint> oldList;

    private final List<EnginePoint> newList;

    public EnginesSortEdit(EngineSlotPainter painter, List<EnginePoint> old, List<EnginePoint> changed) {
        this.pointPainter = painter;
        this.oldList = old;
        this.newList = changed;
    }

    @Override
    public void undo() {
        pointPainter.setEnginePoints(oldList);
        EventBus.publish(new ViewerRepaintQueued());
        EventBus.publish(new EnginesPanelRepaintQueued());
    }

    @Override
    public void redo() {
        pointPainter.setEnginePoints(newList);
        EventBus.publish(new ViewerRepaintQueued());
        EventBus.publish(new EnginesPanelRepaintQueued());
    }

    @Override
    public String getName() {
        return "Sort Engines";
    }

}
