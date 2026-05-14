package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class AuctionRoomController {

    @FXML
    private TableView<?> bidTable;

    private Auction auction;

    @FXML
    public void initialize() {
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }
}
