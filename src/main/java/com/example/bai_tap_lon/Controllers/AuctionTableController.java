package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AuctionTableController {
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> idColumn;
    @FXML private TableColumn<Auction, String> itemColumn;
    @FXML private TableColumn<Auction, String> categoryColumn;
    @FXML private TableColumn<Auction, String> sellerColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
    @FXML private TableColumn<Auction, String> currentBidColumn;
    @FXML private TableColumn<Auction, String> startTimeColumn;
    @FXML private TableColumn<Auction, String> endTimeColumn;
    @FXML private TableColumn<Auction, String> bidCountColumn;

    private AuctionWorkspace workspace;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(AuctionWorkspace.shortId(data.getValue())));
        itemColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItem().getName()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getItem().getClass().getSimpleName()
        ));
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeller().getUsername()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        currentBidColumn.setCellValueFactory(data -> new SimpleStringProperty(
                AuctionWorkspace.money(data.getValue().getItem().getCurrentHighestBid())
        ));
        startTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                AuctionWorkspace.dateTime(data.getValue().getItem().getStartTime())
        ));
        endTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                AuctionWorkspace.dateTime(data.getValue().getItem().getEndTime())
        ));
        bidCountColumn.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getBidHistory().size())
        ));
    }

    public void setWorkspace(AuctionWorkspace workspace) {
        if (this.workspace != null) {
            return;
        }
        this.workspace = workspace;
        auctionTable.setItems(workspace.getAuctions());

        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != workspace.getSelectedAuction()) {
                workspace.selectedAuctionProperty().set(selected);
            }
        });
        workspace.selectedAuctionProperty().addListener((obs, oldValue, selected) -> {
            if (auctionTable.getSelectionModel().getSelectedItem() != selected) {
                auctionTable.getSelectionModel().select(selected);
            }
        });
        workspace.revisionProperty().addListener((obs, oldValue, newValue) -> auctionTable.refresh());

        if (workspace.getSelectedAuction() == null && !workspace.getAuctions().isEmpty()) {
            auctionTable.getSelectionModel().selectFirst();
        } else {
            auctionTable.getSelectionModel().select(workspace.getSelectedAuction());
        }
    }
}
