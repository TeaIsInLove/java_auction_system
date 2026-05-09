package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionBidController {
    @FXML private TextField bidderField;
    @FXML private TextField bidAmountField;
    @FXML private Label selectedAuctionLabel;
    @FXML private Button startButton;
    @FXML private Button bidButton;
    @FXML private Button endButton;

    private AuctionWorkspace workspace;

    public void setWorkspace(AuctionWorkspace workspace) {
        if (this.workspace != null) {
            return;
        }
        this.workspace = workspace;
        workspace.selectedAuctionProperty().addListener((obs, oldValue, selected) -> updateSelection(selected));
        workspace.revisionProperty().addListener((obs, oldValue, newValue) -> updateSelection(workspace.getSelectedAuction()));
        updateSelection(workspace.getSelectedAuction());
    }

    @FXML
    private void handleStartAuction() {
        workspace.startAuction(workspace.getSelectedAuction());
    }

    @FXML
    private void handlePlaceBid() {
        Double amount = parseMoney(bidAmountField.getText());
        if (amount == null || amount <= 0) {
            workspace.showMessage("Gia dat phai la so lon hon 0.", false);
            return;
        }

        boolean success = workspace.placeBid(workspace.getSelectedAuction(), text(bidderField), amount);
        if (success) {
            bidAmountField.clear();
        }
    }

    @FXML
    private void handleEndAuction() {
        workspace.endAuction(workspace.getSelectedAuction());
    }

    private void updateSelection(Auction auction) {
        if (auction == null) {
            selectedAuctionLabel.setText("Chua chon phien");
            setButtonsDisabled(true, true, true);
            return;
        }

        String winningBidder = "Chua co";
        BidTransaction winningBid = auction.getWinningBid();
        if (winningBid != null) {
            winningBidder = winningBid.getBidder().getUsername();
        }

        selectedAuctionLabel.setText(
                auction.getItem().getName()
                        + " | " + auction.getItem().getClass().getSimpleName()
                        + " | " + auction.getStatus()
                        + " | Gia dau: " + AuctionWorkspace.money(auction.getItem().getStartingPrice())
                        + " | Gia hien tai: " + AuctionWorkspace.money(auction.getItem().getCurrentHighestBid())
                        + " | Dan dau: " + winningBidder
        );

        boolean canStart = auction.getStatus() == AuctionStatus.OPEN;
        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING;
        boolean canEnd = auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING;
        setButtonsDisabled(!canStart, !canBid, !canEnd);
    }

    private void setButtonsDisabled(boolean startDisabled, boolean bidDisabled, boolean endDisabled) {
        startButton.setDisable(startDisabled);
        bidButton.setDisable(bidDisabled);
        endButton.setDisable(endDisabled);
    }

    private Double parseMoney(String value) {
        try {
            return Double.parseDouble(value.trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
