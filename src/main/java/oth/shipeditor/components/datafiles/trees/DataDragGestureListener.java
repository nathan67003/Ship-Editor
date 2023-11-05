package oth.shipeditor.components.datafiles.trees;

import oth.shipeditor.components.datafiles.entities.HullmodCSVEntry;
import oth.shipeditor.components.datafiles.entities.ShipCSVEntry;
import oth.shipeditor.components.datafiles.entities.WeaponCSVEntry;
import oth.shipeditor.components.datafiles.entities.WingCSVEntry;
import oth.shipeditor.components.datafiles.entities.transferable.TransferableHullmod;
import oth.shipeditor.components.datafiles.entities.transferable.TransferableShip;
import oth.shipeditor.components.datafiles.entities.transferable.TransferableWeapon;
import oth.shipeditor.components.datafiles.entities.transferable.TransferableWing;
import oth.shipeditor.components.viewer.control.ControlPredicates;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;

/**
 * @author Ontheheavens
 * @since 05.11.2023
 */
public class DataDragGestureListener implements DragGestureListener {

    private final JTree tree;

    DataDragGestureListener(JTree inputTree) {
        this.tree = inputTree;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();

            Transferable transferable;
            switch (userObject) {
                case ShipCSVEntry shipEntry -> transferable = new TransferableShip(shipEntry);
                case WeaponCSVEntry weaponEntry -> transferable = new TransferableWeapon(weaponEntry);
                case HullmodCSVEntry hullmodEntry -> transferable = new TransferableHullmod(hullmodEntry);
                case WingCSVEntry wingEntry -> transferable = new TransferableWing(wingEntry);
                default -> {
                    return;
                }
            }
            ControlPredicates.setDragToViewerInProgress(true);
            dge.startDrag(DragSource.DefaultMoveDrop, transferable);
        }
    }

}
