package oth.shipeditor;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import oth.shipeditor.communication.EventBus;
import oth.shipeditor.communication.events.components.WindowRepaintQueued;
import oth.shipeditor.components.WindowContentPanes;
import oth.shipeditor.components.viewer.ShipViewable;
import oth.shipeditor.menubar.PrimaryMenuBar;

import javax.swing.*;
import java.awt.*;

/**
 * @author Ontheheavens
 * @since 27.04.2023
 */
@Log4j2
public final class PrimaryWindow extends JFrame {

    @Getter
    private final PrimaryMenuBar primaryMenu;

    private final WindowContentPanes contentPanes;

    private PrimaryWindow() {
        log.info("Creating window.");
        this.setTitle("Ship Editor");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));

        this.initListeners();
        // This centers the frame.
        this.setLocationRelativeTo(null);
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        primaryMenu = new PrimaryMenuBar();
        this.setJMenuBar(primaryMenu);

        this.contentPanes = new WindowContentPanes(this.getContentPane());

        contentPanes.loadShipView();
        contentPanes.loadLayerHandling();

        contentPanes.loadEditingPanes();
        contentPanes.dispatchLoaderEvents();

        PrimaryWindow.configureTooltips();

        this.pack();
    }

    ShipViewable getShipView() {
        return this.contentPanes.getShipView();
    }

    public static PrimaryWindow create() {
        return new PrimaryWindow();
    }

    private static void configureTooltips() {
        ToolTipManager.sharedInstance().setInitialDelay(0);
    }

    private void initListeners() {
        EventBus.subscribe(event -> {
            if (event instanceof WindowRepaintQueued) {
                contentPanes.refreshContent();
            }
        });
    }

    void showGUI() {
        this.setVisible(true);
    }

}
