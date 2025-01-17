package oth.shipeditor.representation.weapon;

import lombok.Getter;
import oth.shipeditor.components.datafiles.entities.CSVEntry;
import oth.shipeditor.components.datafiles.entities.WeaponCSVEntry;
import oth.shipeditor.components.viewer.entities.weapon.SlotData;
import oth.shipeditor.utility.text.StringConstants;

import java.awt.*;

/**
 * @author Ontheheavens.
 * @since 26.07.2023
 */
@Getter
public enum WeaponType {

    BALLISTIC("BALLISTIC", "Ballistic", new Color(255,215,0,255)),
    ENERGY("ENERGY", "Energy", new Color(70,200,255,255)),
    MISSILE("MISSILE", "Missile", new Color(155,255,0,255)),
    LAUNCH_BAY(StringConstants.LAUNCH_BAY, "Launch Bay", new Color(255, 255, 255, 255)),
    UNIVERSAL("UNIVERSAL", "Universal", new Color(235, 235, 235,255)),
    HYBRID("HYBRID", "Hybrid", new Color(255,165,0,255)),
    SYNERGY("SYNERGY", "Synergy", new Color(0,255,200,255)),
    COMPOSITE("COMPOSITE", "Composite", new Color(215,255,0,255)),
    BUILT_IN("BUILT_IN", "Built in", new Color(195, 195, 195, 255)),
    DECORATIVE("DECORATIVE", "Decorative", new Color(255, 0, 0,255)),
    SYSTEM("SYSTEM", "System", new Color(145, 145, 145, 255)),
    STATION_MODULE("STATION_MODULE", "Station Module", new Color(170, 0, 255,255));

    private final String id;

    private final Color color;

    private final String displayedName;

    WeaponType(String serialized, String name, Color tone) {
        this.id = serialized;
        this.displayedName = name;
        this.color = tone;
    }

    public static WeaponType value(String textValue) {
        if (textValue == null || textValue.isEmpty()) {
            return null;
        } else return WeaponType.valueOf(textValue);
    }

    /**
     * @param entry only relevant for ship and weapon entries.
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static boolean isValidForSlot(SlotData slotPoint, CSVEntry entry) {
        if (entry instanceof WeaponCSVEntry weaponEntry) {
            return WeaponType.isWeaponFitting(slotPoint, weaponEntry);
        } else {
            WeaponType slotPointType = slotPoint.getWeaponType();
            return slotPointType == WeaponType.STATION_MODULE;
        }
    }

    /**
     * Yeah, this sucks, but... weapon type rules suck in the first place, sorry Alex!
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression", "RedundantLabeledSwitchRuleCodeBlock", "OverlyComplexMethod"})
    public static boolean isWeaponFitting(SlotData slotPoint, WeaponCSVEntry weaponEntry) {
        WeaponType slotType = slotPoint.getWeaponType();
        WeaponType weaponType = weaponEntry.getType();

        int sizeDifference = WeaponSize.getSizeDifference(slotPoint.getWeaponSize(), weaponEntry.getSize());
        boolean isSameOrSmaller = 1 >= sizeDifference && sizeDifference >= 0;
        boolean isSameSize = sizeDifference == 0;

        boolean result;

        switch (slotType) {
            case BALLISTIC -> {
                result = (weaponType == WeaponType.BALLISTIC && isSameOrSmaller)
                        || (weaponType == WeaponType.HYBRID && isSameSize)
                        || (weaponType == WeaponType.COMPOSITE && isSameSize);
            }
            case ENERGY -> {
                result = (weaponType == WeaponType.ENERGY && isSameOrSmaller)
                        || (weaponType == WeaponType.HYBRID && isSameSize)
                        || (weaponType == WeaponType.SYNERGY && isSameSize);
            }
            case MISSILE -> {
                result = (weaponType == WeaponType.MISSILE && isSameOrSmaller)
                        || (weaponType == WeaponType.COMPOSITE && isSameSize)
                        || (weaponType == WeaponType.SYNERGY && isSameSize);
            }
            case HYBRID -> {
                result = (weaponType == WeaponType.HYBRID && isSameOrSmaller)
                        || (weaponType == WeaponType.BALLISTIC && isSameSize)
                        || (weaponType == WeaponType.ENERGY && isSameSize);
            }
            case COMPOSITE -> {
                result = (weaponType == WeaponType.COMPOSITE && isSameOrSmaller)
                        || (weaponType == WeaponType.BALLISTIC && isSameSize)
                        || (weaponType == WeaponType.MISSILE && isSameSize);
            }
            case SYNERGY -> {
                result = (weaponType == WeaponType.SYNERGY && isSameOrSmaller)
                        || (weaponType == WeaponType.ENERGY && isSameSize)
                        || (weaponType == WeaponType.MISSILE && isSameSize);
            }
            case UNIVERSAL -> {
                result = (weaponType == WeaponType.UNIVERSAL && isSameOrSmaller)
                        || (weaponType != WeaponType.LAUNCH_BAY
                        && weaponType != WeaponType.BUILT_IN
                        && weaponType != WeaponType.DECORATIVE
                        && weaponType != WeaponType.SYSTEM
                        && weaponType != WeaponType.STATION_MODULE
                        && isSameSize);
            }
            case BUILT_IN -> result = (weaponType != WeaponType.DECORATIVE && isSameOrSmaller);
            case DECORATIVE -> result = (weaponType == WeaponType.DECORATIVE && isSameOrSmaller);
            default -> {
                result = false;
            }
        }
        return result;
    }

}
