package oth.shipeditor.components.datafiles;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.components.datafiles.entities.ShipCSVEntry;
import oth.shipeditor.menubar.Files;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author Ontheheavens
 * @since 18.06.2023
 */
@Log4j2
public class GameDataPanel extends JPanel {

    private JPanel entryDataPanel;

    public GameDataPanel() {
        this.setLayout(new BorderLayout());
        JPanel topContainer = new JPanel();
        topContainer.add(new JLabel("Game data"));
        JButton loadCSVButton = new JButton(new LoadHullDataAction());
        loadCSVButton.setActionCommand(Files.STARSECTOR_CORE);
        loadCSVButton.setText("Load ship data");
        topContainer.add(loadCSVButton);
        this.add(topContainer, BorderLayout.PAGE_START);
        JSplitPane splitPane = createContentSplitter();
        this.add(splitPane, BorderLayout.CENTER);
    }

    private JSplitPane createContentSplitter() {
        HullsTree hullsTree = new HullsTree(this);
        entryDataPanel = new JPanel();
        entryDataPanel.add(new JLabel("Entry Content"));
        entryDataPanel.setLayout(new BoxLayout(entryDataPanel, BoxLayout.PAGE_AXIS));
        entryDataPanel.setAlignmentX(CENTER_ALIGNMENT);
        entryDataPanel.setBorder(BorderFactory.createEmptyBorder(2,6, 2, 2));
        JScrollPane dataScrollContainer = new JScrollPane(entryDataPanel);
        dataScrollContainer.setBorder(BorderFactory.createEmptyBorder());
        JSplitPane treeSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        treeSplitter.setOneTouchExpandable(true);
        treeSplitter.setResizeWeight(0.4f);
        treeSplitter.setLeftComponent(hullsTree);
        treeSplitter.setRightComponent(dataScrollContainer);
        return treeSplitter;
    }

    void updateEntryPanel(ShipCSVEntry selected) {
        entryDataPanel.removeAll();
        Map<String, String> data = selected.getRowData();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            JLabel keyLabel = new JLabel(entry.getKey() + ": " + entry.getValue());
            entryDataPanel.add(keyLabel);
        }
        entryDataPanel.revalidate();
        entryDataPanel.repaint();
    }

}
