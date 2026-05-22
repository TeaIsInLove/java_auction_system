package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BidHistoryController {

    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();
    private String currentUsername;
    private ObservableList<BidHistoryItem> bidHistoryItems = FXCollections.observableArrayList();

    // FXML Elements
    @FXML private Label topUsernameLabel;
    @FXML private Label balanceLabel;
    @FXML private Label sidebarUsernameLabel;

    // Stats
    @FXML private Label totalBidsLabel;
    @FXML private Label wonAuctionsLabel;
    @FXML private Label lostAuctionsLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label winRateLabel;
    @FXML private Label totalLabel;

    // Filter
    @FXML private ComboBox<String> filterCombo;
    @FXML private VBox bidHistoryContainer;
    @FXML private VBox emptyState;

    // Bid history item class
    public static class BidHistoryItem {
        public Auction auction;
        public double myBidAmount;
        public double currentPrice;
        public String result; // LEADING, OUTBID, WON, LOST
        public LocalDateTime lastBidTime;
        public boolean isHighestBid;

        public BidHistoryItem(Auction auction, double myBidAmount, double currentPrice,
                             String result, LocalDateTime lastBidTime, boolean isHighestBid) {
            this.auction = auction;
            this.myBidAmount = myBidAmount;
            this.currentPrice = currentPrice;
            this.result = result;
            this.lastBidTime = lastBidTime;
            this.isHighestBid = isHighestBid;
        }
    }

    @FXML
    public void initialize() {
        workspace.initialize();

        currentUsername = SessionManager.getInstance().getUsername();
        topUsernameLabel.setText(currentUsername);
        sidebarUsernameLabel.setText(currentUsername);

        setupFilter();
        loadBalance();
        loadBidHistory();
        loadStats();
        setupListeners();
    }

    private void setupFilter() {
        filterCombo.setItems(FXCollections.observableArrayList(
                "Tất cả",
                "Đang tham gia",
                "Đã thắng",
                "Đã thua"
        ));
        filterCombo.setValue("Tất cả");
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void setupListeners() {
        workspace.revisionProperty().addListener((obs, oldV, newV) -> {
            loadBidHistory();
            loadStats();
            loadBalance();
        });
    }

    private void loadBalance() {
        AppUser user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            double freshBalance = workspace.getUserBalance(user.getUsername());
            balanceLabel.setText(AuctionWorkspace.formatVnd(freshBalance));
        } else {
            balanceLabel.setText("0");
        }
    }

    private void loadBidHistory() {
        List<BidHistoryItem> allItems = buildBidHistoryItems();

        bidHistoryItems.setAll(allItems);
        applyFilter();
    }

    private List<BidHistoryItem> buildBidHistoryItems() {
        List<BidHistoryItem> items = new ArrayList<>();

        for (Auction auction : workspace.getAuctions()) {
            // Find all bids by current user in this auction
            List<BidTransaction> myBids = auction.getBidHistory().stream()
                    .filter(b -> b.getBidder().getUsername().equals(currentUsername))
                    .sorted(Comparator.comparing(BidTransaction::getBidTime).reversed())
                    .collect(Collectors.toList());

            if (!myBids.isEmpty()) {
                double myHighestBid = myBids.get(0).getBidAmount();
                double currentPrice = auction.getItem().getCurrentHighestBid();
                LocalDateTime lastBidTime = myBids.get(0).getBidTime();

                // Determine result
                String result;
                if (auction.getStatus() == AuctionStatus.PAID ||
                    auction.getStatus() == AuctionStatus.FINISHED) {
                    if (auction.getWinningBid() != null &&
                        auction.getWinningBid().getBidder().getUsername().equals(currentUsername)) {
                        result = "WON";
                    } else {
                        result = "LOST";
                    }
                } else {
                    // Auction is still running
                    if (myHighestBid >= currentPrice) {
                        result = "LEADING";
                    } else {
                        result = "OUTBID";
                    }
                }

                items.add(new BidHistoryItem(
                        auction,
                        myHighestBid,
                        currentPrice,
                        result,
                        lastBidTime,
                        myHighestBid >= currentPrice
                ));
            }
        }

        // Sort by last bid time (newest first)
        items.sort((a, b) -> {
            if (a.lastBidTime == null && b.lastBidTime == null) return 0;
            if (a.lastBidTime == null) return 1;
            if (b.lastBidTime == null) return -1;
            return b.lastBidTime.compareTo(a.lastBidTime);
        });

        return items;
    }

    private void applyFilter() {
        String filter = filterCombo.getValue();
        if (filter == null) filter = "Tất cả";

        List<BidHistoryItem> filtered;
        switch (filter) {
            case "Đang tham gia" -> filtered = bidHistoryItems.stream()
                    .filter(i -> i.result.equals("LEADING") || i.result.equals("OUTBID"))
                    .collect(Collectors.toList());
            case "Đã thắng" -> filtered = bidHistoryItems.stream()
                    .filter(i -> i.result.equals("WON"))
                    .collect(Collectors.toList());
            case "Đã thua" -> filtered = bidHistoryItems.stream()
                    .filter(i -> i.result.equals("LOST"))
                    .collect(Collectors.toList());
            default -> filtered = new ArrayList<>(bidHistoryItems);
        }

        renderBidHistory(filtered);
        totalLabel.setText("Tổng: " + filtered.size() + " bản ghi");
    }

    private void renderBidHistory(List<BidHistoryItem> items) {
        bidHistoryContainer.getChildren().clear();

        if (items.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (BidHistoryItem item : items) {
            VBox card = createBidHistoryCard(item);
            bidHistoryContainer.getChildren().add(card);
        }
    }

    private VBox createBidHistoryCard(BidHistoryItem item) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("bid-history-card");

        // Result indicator bar
        HBox statusBar = new HBox();
        statusBar.setPrefHeight(4);
        String barColor = switch (item.result) {
            case "LEADING" -> "#27ae60";
            case "OUTBID" -> "#e74c3c";
            case "WON" -> "#27ae60";
            case "LOST" -> "#95a5a6";
            default -> "#5d6d7e";
        };
        statusBar.setStyle("-fx-background-color: " + barColor + ";");

        // Main content
        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-background-color: white; -fx-padding: 15;");

        // Left: Product Info
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(item.auction.getItem().getName());
        nameLabel.getStyleClass().add("auction-name");

        Label categoryLabel = new Label(item.auction.getItem().getItemCategory());
        categoryLabel.getStyleClass().add("category-tag");

        nameRow.getChildren().addAll(nameLabel, categoryLabel);

        // Seller info
        Label sellerLabel = new Label("👤 " + (item.auction.getSeller() != null ?
                item.auction.getSeller().getUsername() : "N/A"));
        sellerLabel.getStyleClass().add("meta-label");

        // Time
        String timeStr = item.lastBidTime != null ?
                item.lastBidTime.toLocalDate().toString() + " " +
                item.lastBidTime.toLocalTime().toString().substring(0, 5) : "N/A";
        Label timeLabel = new Label("🕐 " + timeStr);
        timeLabel.getStyleClass().add("meta-label");

        infoBox.getChildren().addAll(nameRow, sellerLabel, timeLabel);

        // Center: Bid Info
        VBox bidBox = new VBox(5);
        bidBox.setAlignment(Pos.CENTER);
        bidBox.setMinWidth(150);

        Label myBidTitle = new Label("Giá bạn đã bid");
        myBidTitle.getStyleClass().add("price-title");
        Label myBidValue = new Label(AuctionWorkspace.formatVnd(item.myBidAmount) + " đ");
        myBidValue.getStyleClass().add("my-bid-amount");

        Label currentTitle = new Label("Giá hiện tại");
        currentTitle.getStyleClass().add("price-title");
        Label currentValue = new Label(AuctionWorkspace.formatVnd(item.currentPrice) + " đ");
        currentValue.getStyleClass().add("current-price-amount");

        bidBox.getChildren().addAll(myBidTitle, myBidValue, currentTitle, currentValue);

        // Right: Result Badge
        VBox resultBox = new VBox(5);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setMinWidth(120);

        String emoji, resultText, resultStyle;
        switch (item.result) {
            case "LEADING" -> {
                emoji = "🟢";
                resultText = "Đang dẫn đầu";
                resultStyle = "result-leading";
            }
            case "OUTBID" -> {
                emoji = "🔴";
                resultText = "Bị vượt giá";
                resultStyle = "result-outbid";
            }
            case "WON" -> {
                emoji = "🏆";
                resultText = "Đã thắng";
                resultStyle = "result-won";
            }
            case "LOST" -> {
                emoji = "❌";
                resultText = "Đã thua";
                resultStyle = "result-lost";
            }
            default -> {
                emoji = "⚪";
                resultText = "Không xác định";
                resultStyle = "result-default";
            }
        }

        Label resultEmoji = new Label(emoji + " " + resultText);
        resultEmoji.getStyleClass().add(resultStyle);

        // Status
        String statusText = switch (item.auction.getStatus()) {
            case RUNNING -> "Đang diễn ra";
            case OPEN -> "Sắp bắt đầu";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().add("auction-status-label");

        resultBox.getChildren().addAll(resultEmoji, statusLabel);

        content.getChildren().addAll(infoBox, bidBox, resultBox);

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-background-color: #f5f0e8; -fx-padding: 10 15;");

        Button viewBtn = new Button("👁 Xem chi tiết");
        viewBtn.getStyleClass().add("action-btn-view");

        Button rebidBtn = new Button("💰 Bid lại");
        rebidBtn.getStyleClass().add("action-btn-rebid");
        if (item.auction.getStatus() != AuctionStatus.RUNNING) {
            rebidBtn.setDisable(true);
        }

        actions.getChildren().addAll(viewBtn, rebidBtn);

        card.getChildren().addAll(statusBar, content, actions);

        viewBtn.setOnAction(e -> openAuctionRoom(item.auction));
        rebidBtn.setOnAction(e -> openAuctionRoom(item.auction));

        return card;
    }

    private void loadStats() {
        int totalBids = bidHistoryItems.size();
        int won = (int) bidHistoryItems.stream()
                .filter(i -> i.result.equals("WON")).count();
        int lost = (int) bidHistoryItems.stream()
                .filter(i -> i.result.equals("LOST")).count();
        double totalSpent = bidHistoryItems.stream()
                .filter(i -> i.result.equals("WON"))
                .mapToDouble(i -> i.currentPrice)
                .sum();

        double winRate = (won + lost) > 0 ? (double) won / (won + lost) * 100 : 0;

        totalBidsLabel.setText(String.valueOf(totalBids));
        wonAuctionsLabel.setText(String.valueOf(won));
        lostAuctionsLabel.setText(String.valueOf(lost));
        totalSpentLabel.setText(AuctionWorkspace.formatVnd(totalSpent) + " đ");
        winRateLabel.setText(String.format("%.1f%%", winRate));
    }

    private void openAuctionRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/AuctionRoom.fxml")
            );

            Parent root = loader.load();

            AuctionRoomController controller = loader.getController();
            controller.setAuction(auction);

            Stage stage = new Stage();
            stage.setTitle(auction.getItem().getName());
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                controller.dispose();
                loadBidHistory();
                loadStats();
            });
            controller.initializeView();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleShowLiveAuctions() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/Auction.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowAllAuctions() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/Auction.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowMyAuctions() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/MyAuctions.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
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

            AuctionCreateController controller = loader.getController();
            controller.setWorkspace(workspace);

            Stage stage = new Stage();
            stage.setTitle("Tạo phiên đấu giá");
            stage.setScene(new Scene(root));
            stage.initOwner(topUsernameLabel.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadBidHistory();
            loadStats();

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
            stage.setTitle("Thông Tin Cá Nhân");
            stage.setScene(new Scene(root));
            stage.initOwner(topUsernameLabel.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadBalance();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/login.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng Nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
