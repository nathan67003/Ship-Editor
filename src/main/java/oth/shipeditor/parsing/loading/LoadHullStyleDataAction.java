package oth.shipeditor.parsing.loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.HullStylesLoaded;
import oth.shipeditor.menubar.FileUtilities;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.HullStyle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ontheheavens
 * @since 01.08.2023
 */
@Log4j2
public class LoadHullStyleDataAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        Path targetFile = Paths.get("data", "config", "hull_styles.json");

        Map<Path, File> hullStyleFiles = FileUtilities.getFileFromPackages(targetFile);

        Map<String, HullStyle> collectedHullStyles = new LinkedHashMap<>();
        for (Map.Entry<Path, File> entry : hullStyleFiles.entrySet()) {
            File styleFile = entry.getValue();
            log.info("Hullstyle data file found in mod directory: {}", entry.getKey());
            Map<String, HullStyle> stylesFromFile = LoadHullStyleDataAction.loadHullStyleFile(styleFile);
            for (HullStyle style : stylesFromFile.values()) {
                style.setContainingPackage(entry.getKey());
            }
            collectedHullStyles.putAll(stylesFromFile);
        }
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setAllHullStyles(collectedHullStyles);
        EventBus.publish(new HullStylesLoaded(collectedHullStyles));
    }

    private static Map<String, HullStyle> loadHullStyleFile(File styleFile) {
        ObjectMapper mapper = FileLoading.getConfigured();
        Map<String, HullStyle> hullStyles = null;
        log.info("Fetching hullstyle data at: {}..", styleFile.toPath());
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            hullStyles = mapper.readValue(styleFile,
                    typeFactory.constructMapType(HashMap.class, String.class, HullStyle.class));

            for (Map.Entry<String, HullStyle> entry : hullStyles.entrySet()) {
                String hullStyleID = entry.getKey();
                HullStyle hullStyle = entry.getValue();
                hullStyle.setHullStyleID(hullStyleID);
                hullStyle.setFilePath(styleFile.toPath());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hullStyles;
    }

}
