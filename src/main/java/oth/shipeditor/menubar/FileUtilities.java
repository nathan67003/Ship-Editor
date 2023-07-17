package oth.shipeditor.menubar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.files.HullFileOpened;
import oth.shipeditor.communication.events.files.SpriteOpened;
import oth.shipeditor.communication.events.viewer.layers.LastLayerSelectQueued;
import oth.shipeditor.communication.events.viewer.layers.LayerCreationQueued;
import oth.shipeditor.components.viewer.layers.ShipLayer;
import oth.shipeditor.parsing.loading.*;
import oth.shipeditor.persistence.Settings;
import oth.shipeditor.persistence.SettingsManager;
import oth.shipeditor.representation.GameDataRepository;
import oth.shipeditor.representation.Hull;
import oth.shipeditor.representation.HullStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Ontheheavens
 * @since 09.05.2023
 */
@Log4j2
public final class FileUtilities {

    public static final String STARSECTOR_CORE = "starsector-core";

    @Getter
    private static final Action loadShipDataAction = new LoadShipDataAction();

    @Getter
    private static final Action loadHullmodDataAction = new LoadHullmodDataAction();

    @Getter
    private static final Action openSpriteAction = new OpenSpriteAction();

    @Getter
    private static final Action openShipDataAction = new OpenHullAction();

    @Getter
    private static final Action loadHullAsLayerAction = new LoadHullAsLayer();

    private FileUtilities() {}

    public static void updateActionStates(ShipLayer current) {
        boolean spriteState = (current != null) && current.getShipSprite() == null && current.getShipData() == null;
        boolean hullState = (current != null) && current.getShipSprite() != null && current.getShipData() == null;
        openSpriteAction.setEnabled(spriteState);
        openShipDataAction.setEnabled(hullState);
    }

    public static void openPathInDesktop(Path toOpen) {
        FileUtilities.openPathInDesktop(toOpen.toFile());
    }

    private static void openPathInDesktop(File toOpen) {
        try {
            Desktop.getDesktop().open(toOpen);
        } catch (IOException | IllegalArgumentException ex) {
            log.error("Failed to open {} in Explorer!", toOpen);
            JOptionPane.showMessageDialog(null,
                    "Failed to open file in Explorer, exception thrown at: " + toOpen,
                    "File interaction error!",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void createLayerWithSprite(File spriteFile) {
        EventBus.publish(new LayerCreationQueued());
        EventBus.publish(new LastLayerSelectQueued());
        BufferedImage sprite = FileLoading.loadSprite(spriteFile);
        EventBus.publish(new SpriteOpened(sprite, spriteFile.getName()));
    }

    private static class OpenHullAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            FileLoading.openHullAndDo(e1 -> {
                    JFileChooser shipDataChooser = (JFileChooser) e1.getSource();
                    File file = shipDataChooser.getSelectedFile();
                    Hull hull = FileLoading.loadHullFile(file);
                    EventBus.publish(new HullFileOpened(hull, file.getName()));
            });
        }
    }

    public static void loadHullStyles() {
        Settings settings = SettingsManager.getSettings();

        Path targetFile = Paths.get("data", "config", "hull_styles.json");

        List<Path> allModFolders = SettingsManager.getAllModFolders();

        Path coreFilePath = Paths.get(settings.getCoreFolderPath()).resolve(targetFile);

        Collection<File> hullStyleFiles = new ArrayList<>();
        hullStyleFiles.add(coreFilePath.toFile());

        try (Stream<Path> childDirectories = allModFolders.stream()) {
            childDirectories.forEach(childDir -> {
                Path targetFilePath = childDir.resolve(targetFile);
                if (Files.exists(targetFilePath)) {
                    hullStyleFiles.add(targetFilePath.toFile());
                }
            });
        }

        Map<String, HullStyle> collectedHullStyles = new HashMap<>();
        for (File styleFile : hullStyleFiles) {
            collectedHullStyles.putAll(FileUtilities.loadHullStyleFile(styleFile));
        }
        GameDataRepository gameData = SettingsManager.getGameData();
        gameData.setAllHullStyles(collectedHullStyles);
    }

    private static Map<String, HullStyle> loadHullStyleFile(File styleFile) {
        ObjectMapper mapper = FileLoading.getConfigured();
        Map<String, HullStyle> hullStyles = null;
        try {
            TypeFactory typeFactory = mapper.getTypeFactory();
            hullStyles = mapper.readValue(styleFile,
                    typeFactory.constructMapType(HashMap.class, String.class, HullStyle.class));

            for (Map.Entry<String, HullStyle> entry : hullStyles.entrySet()) {
                String hullStyleID = entry.getKey();
                HullStyle hullStyle = entry.getValue();
                hullStyle.setHullStyleID(hullStyleID);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hullStyles;
    }

}
