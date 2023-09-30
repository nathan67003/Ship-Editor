package oth.shipeditor.components.viewer.layers.ship.data;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.components.datafiles.entities.ShipCSVEntry;
import oth.shipeditor.components.datafiles.entities.WeaponCSVEntry;
import oth.shipeditor.components.viewer.layers.ship.ShipPainter;
import oth.shipeditor.components.viewer.layers.weapon.WeaponPainter;
import oth.shipeditor.components.viewer.painters.points.ship.features.FireMode;
import oth.shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import oth.shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import oth.shipeditor.representation.*;
import oth.shipeditor.representation.weapon.WeaponSpecFile;
import oth.shipeditor.utility.text.StringValues;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heavy-footprint runtime variant class; stores full-fledged painters and point indexes for display in viewer.
 * @author Ontheheavens
 * @since 28.08.2023
 */
@Getter @Setter
public class ShipVariant implements Variant {

    public static final String EMPTY_VARIANT = "Empty variant";

    /**
     * Signifies that the instance is a placeholder variant, not a variant without anything installed;
     * Variants that don't have weapons yet but can install them are normal variants.
     */
    private boolean empty;

    private Path variantFilePath;

    private Path containingPackage;

    /**
     * Can be either ID of base hull or skin hull ID.
     */
    private String shipHullId;

    private String variantId;

    private String displayName;

    private Map<String, InstalledFeature> fittedModules;

    private List<FittedWeaponGroup> weaponGroups;

    public ShipVariant() {
        this(true);
    }

    @SuppressWarnings("BooleanParameter")
    public ShipVariant(boolean isEmpty) {
        this.empty = isEmpty;
    }

    public String getFileName() {
        if (variantFilePath == null) return StringValues.EMPTY;
        return variantFilePath.getFileName().toString();
    }

    public void ensureBuiltInsAreFitted(ShipPainter painter) {
        var builtIns = painter.getBuiltInsWithSkin(false, true);
        builtIns.forEach((slotID, feature) -> {
            if (!this.isBuiltInFitted(feature)) {
                FittedWeaponGroup firstGroup = weaponGroups.get(0);
                if (firstGroup != null) {
                    var groupWeapons = firstGroup.getWeapons();
                    groupWeapons.put(slotID, feature);
                } else {
                    FittedWeaponGroup newGroup = new FittedWeaponGroup(this,
                            false, FireMode.LINKED);
                    this.weaponGroups.add(newGroup);
                    var groupWeapons = newGroup.getWeapons();
                    groupWeapons.put(slotID, feature);
                }
            }
        });
    }

    private boolean isBuiltInFitted(InstalledFeature builtIn) {
        boolean result = false;
        for (FittedWeaponGroup group : weaponGroups) {
            var groupWeapons = group.getWeapons();
            var weaponList = groupWeapons.valueList();
            for (InstalledFeature weapon : weaponList) {
                if (weapon == builtIn) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

        public Map<String, InstalledFeature> getAllFittedWeapons() {
        Map<String, InstalledFeature> result = new LinkedHashMap<>();
        for (FittedWeaponGroup weaponGroup : weaponGroups) {
            var weaponsInGroup = weaponGroup.getWeapons();
            result.putAll(weaponsInGroup);
        }
        return result;
    }

    @SuppressWarnings("OverlyCoupledMethod")
    public void initialize(VariantFile file) {
        this.setVariantId(variantId);
        this.setShipHullId(file.getHullId());
        this.setVariantFilePath(file.getVariantFilePath());
        this.setContainingPackage(file.getContainingPackage());
        this.setDisplayName(file.getDisplayName());

        weaponGroups = new ArrayList<>();

        List<SpecWeaponGroup> specWeaponGroups = file.getWeaponGroups();
        specWeaponGroups.forEach(weaponGroup -> {
            String weaponGroupMode = weaponGroup.getMode();
            FireMode mode = FireMode.valueOf(weaponGroupMode);
            FittedWeaponGroup initialized = new FittedWeaponGroup(this, weaponGroup.isAutofire(), mode);
            var fitted = initialized.getWeapons();
            Map<String, String> specGroupWeapons = weaponGroup.getWeapons();
            specGroupWeapons.forEach((slotID, weaponID) -> {
                WeaponCSVEntry weaponEntry = GameDataRepository.getWeaponByID(weaponID);
                WeaponSpecFile specFile = weaponEntry.getSpecFile();
                WeaponPainter weaponPainter = weaponEntry.createPainterFromEntry(null, specFile);
                fitted.put(slotID, InstalledFeature.of(slotID, weaponID, weaponPainter, weaponEntry));
            });
            weaponGroups.add(initialized);
        });

        var installedModules = file.getModules();
        if (installedModules != null) {
            fittedModules = new LinkedHashMap<>();
            installedModules.forEach((slotID, variantID) -> {
                VariantFile variant = GameDataRepository.getVariantByID(variantID);
                ShipSpecFile specFile = GameDataRepository.retrieveSpecByID(variant.getHullId());
                String baseHullId;
                SkinSpecFile skinSpec = null;
                if (specFile instanceof SkinSpecFile checkedSkin) {
                    baseHullId = checkedSkin.getBaseHullId();
                    skinSpec = checkedSkin;
                } else {
                    baseHullId = specFile.getHullId();
                }
                ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(baseHullId);
                ShipPainter modulePainter = csvEntry.createPainterFromEntry(null);

                if (skinSpec != null) {
                    ShipSkin shipSkin = ShipSkin.createFromSpec(skinSpec);
                    modulePainter.setActiveSpec(ActiveShipSpec.SKIN, shipSkin);
                }

                modulePainter.selectVariant(variant);
                fittedModules.put(slotID, InstalledFeature.of(slotID, variantID, modulePainter, csvEntry));
            });
        }

    }

    @Override
    public String toString() {
        if (empty) {
            return EMPTY_VARIANT;
        }
        return displayName;
    }

}
