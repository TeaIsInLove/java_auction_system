package com.example.bai_tap_lon.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionController {
    @FXML private Label messageLabel;
    @FXML private AuctionSummaryController summaryViewController;
    @FXML private AuctionTableController tableViewController;
    @FXML private AuctionCreateController createViewController;
    @FXML private AuctionBidController bidViewController;
    @FXML private AuctionLogController logViewController;

    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();

    @FXML
    public void initialize() {
        workspace.initialize();

        summaryViewController.setWorkspace(workspace);
        tableViewController.setWorkspace(workspace);
        createViewController.setWorkspace(workspace);
        bidViewController.setWorkspace(workspace);
        logViewController.setWorkspace(workspace);

        messageLabel.textProperty().bind(workspace.messageProperty());
        workspace.successMessageProperty().addListener((obs, oldValue, success) -> applyMessageStyle(success));
        applyMessageStyle(workspace.successMessageProperty().get());
    }

    @FXML
    private void handleLoadSamples() {
        workspace.loadSampleAuctions();
    }

    private void applyMessageStyle(boolean success) {
        messageLabel.setStyle(success ? "-fx-text-fill: #047857;" : "-fx-text-fill: #b91c1c;");
    }
}
