package oth.shipeditor.components.viewer.entities.bays;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.components.viewer.painters.points.LaunchBayPainter;
import oth.shipeditor.representation.weapon.WeaponMount;
import oth.shipeditor.representation.weapon.WeaponSize;
import oth.shipeditor.representation.weapon.WeaponType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ontheheavens
 * @since 13.08.2023
 */
@Getter
public class LaunchBay {

    @Setter
    private String id;

    @Setter
    private WeaponSize weaponSize = WeaponSize.SMALL;

    private final WeaponType weaponType = WeaponType.LAUNCH_BAY;

    @Setter
    private WeaponMount weaponMount = WeaponMount.HIDDEN;

    @Setter
    private int renderOrderMod;

    @Setter
    private double arc;

    @Setter
    private double angle;

    private final List<LaunchPortPoint> portPoints;

    private final LaunchBayPainter bayPainter;

    public LaunchBay(String inputID, LaunchBayPainter painter) {
        this.id = inputID;
        this.bayPainter = painter;
        this.portPoints = new ArrayList<>();
    }

}