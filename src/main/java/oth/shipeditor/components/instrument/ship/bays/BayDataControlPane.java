package oth.shipeditor.components.instrument.ship.bays;

import oth.shipeditor.components.instrument.ship.AbstractSlotValuesPanel;
import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.components.viewer.entities.weapon.SlotPoint;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.representation.weapon.WeaponMount;
import oth.shipeditor.representation.weapon.WeaponSize;
import oth.shipeditor.representation.weapon.WeaponType;
import oth.shipeditor.undo.EditDispatch;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;

/**
 * @author Ontheheavens
 * @since 14.08.2023
 */
public class BayDataControlPane extends AbstractSlotValuesPanel {

    @SuppressWarnings("TypeMayBeWeakened")
    BayDataControlPane(SlotPoint slotPoint) {
        super(slotPoint, false);
    }

    @Override
    protected String getEntityName() {
        return "Bay";
    }

    @Override
    protected void addContent() {
        super.addIDPanel();

        super.addMountSelector();
        super.addSizeSelector();

        super.addAngleController();
        super.addArcController();

        super.addRenderOrderController();
    }

    @Override
    protected String getNextUniqueID(ShipPainter shipPainter) {
        var bayPainter = shipPainter.getBayPainter();
        return bayPainter.generateUniqueBayID();
    }

    @Override
    protected ActionListener getTypeSelectorListener(JComboBox<WeaponType> typeSelector) {
        throw new UnsupportedOperationException("Type selection is not relevant for BayDataControl!");
    }

    @Override
    protected ActionListener getMountSelectorListener(JComboBox<WeaponMount> mountSelector) {
        return e -> {
            WeaponMount selectedMount = (WeaponMount) mountSelector.getSelectedItem();
            EditDispatch.postSlotMountChanged( getSelected(), selectedMount);
        };
    }

    @Override
    protected ActionListener getSizeSelectorListener(JComboBox<WeaponSize> sizeSelector) {
        return e -> {
            WeaponSize selectedSize = (WeaponSize) sizeSelector.getSelectedItem();
            EditDispatch.postSlotSizeChanged( getSelected(), selectedSize);
        };
    }

    @Override
    protected ChangeListener getAngleChangeListener(JSpinner spinner, SpinnerNumberModel spinnerNumberModel, SlotData slotPoint) {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number modelNumber = spinnerNumberModel.getNumber();
                double current = modelNumber.doubleValue();
                EditDispatch.postSlotAngleSet(slotPoint,slotPoint.getAngle(),current);
                spinner.removeChangeListener(this);
            }
        };
    }

    @Override
    protected ChangeListener getArcChangeListener(JSpinner spinner, SpinnerNumberModel spinnerNumberModel, SlotData slotPoint) {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number modelNumber = spinnerNumberModel.getNumber();
                double current = modelNumber.doubleValue();

                EditDispatch.postSlotArcSet(slotPoint,slotPoint.getArc(),current);
                spinner.removeChangeListener(this);
            }
        };
    }

    @Override
    protected ChangeListener getRenderOrderChangeListener(JSpinner spinner,
                                                          SpinnerNumberModel spinnerNumberModel,
                                                          SlotData slotPoint) {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Number modelNumber = spinnerNumberModel.getNumber();
                int current = modelNumber.intValue();

                EditDispatch.postRenderOrderChanged(slotPoint,slotPoint.getRenderOrderMod(),current);
                spinner.removeChangeListener(this);
            }
        };
    }

}
