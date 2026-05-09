package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionSummaryController {
    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningAuctionsLabel;
    @FXML private Label openAuctionsLabel;
    @FXML private Label highestBidLabel;

    private AuctionWorkspace workspace;

    public void setWorkspace(AuctionWorkspace workspace) {
        if (this.workspace != null) {
            return;
        }
        this.workspace = workspace;
        workspace.getAuctions().addListener((ListChangeListener<Auction>) change -> updateSummary());
        workspace.revisionProperty().addListener((obs, oldValue, newValue) -> updateSummary());
        updateSummary();
    }

    private void updateSummary() {
        if (workspace == null) {
            return;
        }

        int total = workspace.getAuctions().size();
        long running = workspace.getAuctions().stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.RUNNING)
                .count();
        long open = workspace.getAuctions().stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.OPEN)
                .count();
        double highest = workspace.getAuctions().stream()
                .mapToDouble(auction -> auction.getItem().getCurrentHighestBid())
                .max()
                .orElse(0.0);

        totalAuctionsLabel.setText(String.valueOf(total));
        runningAuctionsLabel.setText(String.valueOf(running));
        openAuctionsLabel.setText(String.valueOf(open));
        highestBidLabel.setText(AuctionWorkspace.money(highest));
    }
}
