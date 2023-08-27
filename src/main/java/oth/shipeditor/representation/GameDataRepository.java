package oth.shipeditor.representation;

import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.HullmodDataSet;
import oth.shipeditor.communication.events.files.WingDataSet;
import oth.shipeditor.components.datafiles.entities.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ontheheavens
 * @since 08.07.2023
 */
@SuppressWarnings("ClassWithTooManyFields")
@Getter
public class GameDataRepository {

    /**
     * All ship entries by their hull IDs.
     */
    private final Map<String, ShipCSVEntry> allShipEntries;

    /**
     * All hullmod entries by their IDs.
     */
    private final Map<String, HullmodCSVEntry> allHullmodEntries;

    /**
     * All shipsystem entries by their IDs.
     */
    private final Map<String, ShipSystemCSVEntry> allShipsystemEntries;

    private final Map<String, WingCSVEntry> allWingEntries;

    private final Map<String, WeaponCSVEntry> allWeaponEntries;

    /**
     * Hull styles by their IDs (field names in JSON).
     */
    @Setter
    private Map<String, HullStyle> allHullStyles;

    /**
     * Engine styles by their IDs (field names in JSON).
     */
    @Setter
    private Map<String, EngineStyle> allEngineStyles;

    /**
     * All variant files by variant IDs.
     */
    @Setter
    private Map<String, Variant> allVariants;

    @Setter
    private boolean shipDataLoaded;

    private boolean hullmodDataLoaded;

    @Setter
    private boolean shipsystemDataLoaded;

    private boolean wingDataLoaded;

    @Setter
    private boolean weaponsDataLoaded;

    public GameDataRepository() {
        this.allShipEntries = new HashMap<>();
        this.allHullmodEntries = new HashMap<>();
        this.allShipsystemEntries = new HashMap<>();
        this.allWingEntries = new HashMap<>();
        this.allWeaponEntries = new HashMap<>();
    }

    public void setHullmodDataLoaded(boolean hullmodsLoaded) {
        this.hullmodDataLoaded = hullmodsLoaded;
        EventBus.publish(new HullmodDataSet());
    }

    public void setWingDataLoaded(boolean wingsLoaded) {
        this.wingDataLoaded = wingsLoaded;
        EventBus.publish(new WingDataSet());
    }

}