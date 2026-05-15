package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.Services.DashboardService;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class AuctionController {

    @FXML private Label topUsernameLabel;

    @FXML private Label sidebarUsernameLabel;

    @FXML private VBox auctionContainer;

    @FXML private Label joinedAuctionLabel;

    @FXML private Label wonAuctionLabel;

    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {

        workspace.initialize();

        String username = SessionManager
                .getInstance()
                .getUsername();

        topUsernameLabel.setText(username);
        sidebarUsernameLabel.setText(username);

        refreshAuctionUI();
        loadStatistics();

        workspace.getAuctions().addListener(
                (ListChangeListener<Auction>) c -> {
                    refreshAuctionUI();

                    loadStatistics();

                }
        );

        workspace.revisionProperty().addListener((obs, oldV, newV) -> loadStatistics());
    }

    private void loadStatistics() {
        String username = SessionManager.getInstance().getUsername();
        if (!SessionManager.getInstance().isLoggedIn()) {
            joinedAuctionLabel.setText("0");
            wonAuctionLabel.setText("0");
            return;
        }

        int joined = dashboardService.getJoinedAuctionCount(username);
        int won = dashboardService.getWonAuctionCount(username);

        joinedAuctionLabel.setText(String.valueOf(joined));
        wonAuctionLabel.setText(String.valueOf(won));
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
                        + AuctionWorkspace.formatVnd(auction.getItem().getStartingPrice())
                        + " VNĐ"
        );

        price.getStyleClass().add("auction-price");

        Label seller = new Label(
                "Người tạo: " + (auction.getSeller() != null ? auction.getSeller().getUsername() : "Không xác định")
        );

        seller.getStyleClass().add("auction-price");

        HBox buttonBox = new HBox(10);

        Button joinButton = new Button("Tham gia phiên");
        joinButton.getStyleClass().add("bid-button");
        joinButton.setOnAction(e -> openAuctionRoom(auction));

        buttonBox.getChildren().add(joinButton);

        // Admin delete button
        if (SessionManager.getInstance().isAdmin()) {
            Button deleteButton = new Button("Xóa");
            deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
            deleteButton.setOnAction(e -> handleDeleteAuction(auction));
            buttonBox.getChildren().add(deleteButton);
        }

        card.getChildren().addAll(title, price, seller, buttonBox);

        return card;
    }

    private void handleDeleteAuction(Auction auction) {
        if (auction == null) return;
        boolean deleted = workspace.deleteAuction(auction, SessionManager.getInstance().getUsername(), SessionManager.getInstance().isAdmin());
        if (deleted) {
            refreshAuctionUI();
        }
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

            // LẤY CONTROLLER
            AuctionRoomController controller =
                    loader.getController();

            // TRUYỀN AUCTION
            controller.setAuction(auction);

            Stage stage = new Stage();

            stage.setTitle(
                    auction.getItem().getName()
            );

            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> controller.dispose());
            controller.initializeView();

            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuctionController.class.getResource(
                            "/com/example/auctionsystem/Views/fxml/Profile.fxml"
                    )
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Thong Tin Ca Nhan");
            stage.setScene(new Scene(root));
            stage.initOwner(topUsernameLabel.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/Login.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dang Nhap");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}