package com.example.bai_tap_lon.Controllers;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class AuctionLogController {
    @FXML private TextArea activityLogArea;

    private AuctionWorkspace workspace;

    public void setWorkspace(AuctionWorkspace workspace) {
        if (this.workspace != null) {
            return;
        }
        this.workspace = workspace;
        workspace.getActivityLogs().addListener((ListChangeListener<String>) change -> updateLog());
        updateLog();
    }

    @FXML
    private void handleClearLog() {
        workspace.clearLogs();
    }

    private void updateLog() {
        if (workspace == null) {
            return;
        }
        activityLogArea.setText(String.join(System.lineSeparator(), workspace.getActivityLogs()));
        activityLogArea.positionCaret(activityLogArea.getText().length());
    }
}
