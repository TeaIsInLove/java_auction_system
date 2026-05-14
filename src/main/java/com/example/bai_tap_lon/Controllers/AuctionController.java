package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.session.SessionManager;
import com.example.bai_tap_lon.model.auction.Auction;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class AuctionController {

    @FXML
    private Label topUsernameLabel;

    @FXML
    private Label sidebarUsernameLabel;

    @FXML
    private VBox auctionContainer;

    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();

    @FXML
    public void initialize() {

        workspace.initialize();

        String username = SessionManager
                .getInstance()
                .getCurrentUsername();

        topUsernameLabel.setText(username);
        sidebarUsernameLabel.setText(username);

        refreshAuctionUI();

        workspace.getAuctions().addListener(
                (ListChangeListener<Auction>) c -> refreshAuctionUI()
        );
    }

    private void refreshAuctionUI() {

        auctionContainer.getChildren().clear();

        workspace.getAuctions().forEach(auction -> {

            VBox card = createAuctionCard(auction);

            auctionContainer.getChildren().add(card);
        });
    }

    private VBox createAuctionCard(Auction auction) {

        VBox card = new VBox();

        card.setSpacing(8);

        card.getStyleClass().add("auction-item");

        Label title = new Label(
                auction.getItem().getName()
        );

        title.getStyleClass().add("auction-name");

        Label price = new Label(
                "Giá khởi điểm: "
                        + auction.getItem().getStartingPrice()
                        + " VNĐ"
        );

        price.getStyleClass().add("auction-price");

        Button joinButton = new Button("Tham gia phiên");
        joinButton.getStyleClass().add("bid-button");
        joinButton.setOnAction(e -> openAuctionRoom(auction));


        card.getChildren().addAll(title, price, joinButton);

        return card;
    }

    @FXML
    private void handleOpenCreateAuction() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    AuctionController.class.getResource(
                            "/com/example/auctionsystem/Views/fxml/AuctionCreate.fxml"
                    )
            );

            Parent root = loader.load();

            // LẤY CONTROLLER
            AuctionCreateController controller =
                    loader.getController();

            // TRUYỀN WORKSPACE
            controller.setWorkspace(workspace);

            Stage stage = new Stage();

            stage.setTitle("Tạo phiên đấu giá");

            stage.setScene(new Scene(root));

            stage.initOwner(topUsernameLabel.getScene().getWindow());

            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setResizable(false);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // mở phiên đấu giá được chọn
    private void openAuctionRoom(Auction auction) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/auctionsystem/Views/fxml/AuctionRoom.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle(
                    auction.getItem().getName()
            );

            stage.setScene(new Scene(root));

            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}