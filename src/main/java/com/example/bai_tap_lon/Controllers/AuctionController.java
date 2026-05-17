package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.Services.DashboardService;
import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionController {

    // View Mode
    private enum ViewMode { LIVE, ALL }
    private ViewMode currentViewMode = ViewMode.LIVE;

    // Data
    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();
    private final DashboardService dashboardService = new DashboardService();
    private final ObservableList<Auction> filteredAuctions = FXCollections.observableArrayList();
    private Timeline countdownTimer;
    private boolean isAdmin;

    // FXML Elements - Top Bar
    @FXML private Label topUsernameLabel;
    @FXML private Label balanceLabel;
    @FXML private Label sidebarUsernameLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // FXML Elements - Search & Filter
    @FXML private HBox searchFilterBar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label totalAuctionsLabel;

    // FXML Elements - Live Section
    @FXML private VBox liveAuctionsSection;
    @FXML private VBox liveAuctionsContainer;
    @FXML private VBox liveEmptyState;
    @FXML private Label liveCountLabel;

    // FXML Elements - All Section
    @FXML private VBox allAuctionsSection;
    @FXML private VBox allAuctionsContainer;
    @FXML private VBox allEmptyState;
    @FXML private Label allCountLabel;

    @FXML
    public void initialize() {
        workspace.initialize();

        String username = SessionManager.getInstance().getUsername();
        isAdmin = SessionManager.getInstance().isAdmin();
        topUsernameLabel.setText(username);
        sidebarUsernameLabel.setText(username);

        // Load balance from database
        loadBalance();

        // Setup filters
        setupFilters();

        // Load initial view
        showLiveAuctionsView();

        // Setup listeners
        setupListeners();

        // Start countdown timer
        startCountdownTimer();
    }

    private void setupFilters() {
        // Status filter options
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả",
                "Chưa bắt đầu",
                "Đang diễn ra",
                "Đã kết thúc",
                "Đã thanh toán",
                "Đã hủy"
        ));
        statusFilter.setValue("Tất cả");

        // Category filter options
        categoryFilter.setItems(FXCollections.observableArrayList(
                "Tất cả",
                "Xe cộ",
                "Điện tử",
                "Nghệ thuật",
                "Khác"
        ));
        categoryFilter.setValue("Tất cả");

        // Search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Filter listeners
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadStatistics() {

        int total = workspace.getAuctions().size();

        long running = workspace.getAuctions().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .count();

        long finished = workspace.getAuctions().stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                .count();

        System.out.println("Tong phien: " + total);
        System.out.println("Dang dien ra: " + running);
        System.out.println("Da ket thuc: " + finished);
    }

    private void setupListeners() {
        workspace.getAuctions().addListener((ListChangeListener<Auction>) c -> {
            applyFilters();
            loadStatistics();
        });

        workspace.revisionProperty().addListener((obs, oldV, newV) -> {
            loadStatistics();
            loadBalance();
            refreshAuctionCards();
        });
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdowns()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdowns() {
        // Update countdown labels in all cards
        Platform.runLater(() -> {
            for (javafx.scene.Node node : liveAuctionsContainer.getChildren()) {
                if (node instanceof VBox card) {
                    Label countdownLabel = (Label) card.getProperties().get("countdownLabel");
                    Auction auction = (Auction) card.getProperties().get("auction");
                    if (countdownLabel != null && auction != null) {
                        countdownLabel.setText(formatCountdown(auction.getItem().getEndTime()));
                    }
                }
            }
            for (javafx.scene.Node node : allAuctionsContainer.getChildren()) {
                if (node instanceof VBox card) {
                    Label countdownLabel = (Label) card.getProperties().get("countdownLabel");
                    Auction auction = (Auction) card.getProperties().get("auction");
                    if (countdownLabel != null && auction != null) {
                        countdownLabel.setText(formatCountdown(auction.getItem().getEndTime()));
                    }
                }
            }
        });
    }

    private void refreshAuctionCards() {
        if (currentViewMode == ViewMode.LIVE) {
            renderLiveAuctions();
        } else {
            renderAllAuctions();
        }
    }

    // ==================== VIEW SWITCHING ====================

    @FXML
    public void handleShowLiveAuctions() {
        currentViewMode = ViewMode.LIVE;
        showLiveAuctionsView();
    }

    @FXML
    public void handleShowAllAuctions() {
        currentViewMode = ViewMode.ALL;
        showAllAuctionsView();
    }

    private void showLiveAuctionsView() {
        currentViewMode = ViewMode.LIVE;

        // Update page header
        pageTitleLabel.setText("🔥 Phiên đang diễn ra");
        pageSubtitleLabel.setText("Cập nhật theo thời gian thực");

        // Show/hide sections
        liveAuctionsSection.setVisible(true);
        liveAuctionsSection.setManaged(true);
        allAuctionsSection.setVisible(false);
        allAuctionsSection.setManaged(false);
        searchFilterBar.setVisible(false);
        searchFilterBar.setManaged(false);

        // Filter only RUNNING auctions
        filteredAuctions.setAll(
                workspace.getAuctions().stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .collect(Collectors.toList())
        );

        renderLiveAuctions();
    }

    private void showAllAuctionsView() {
        currentViewMode = ViewMode.ALL;

        // Update page header
        pageTitleLabel.setText("📦 Tất cả phiên đấu giá");
        pageSubtitleLabel.setText("Danh sách đầy đủ các phiên");

        // Show/hide sections
        liveAuctionsSection.setVisible(false);
        liveAuctionsSection.setManaged(false);
        allAuctionsSection.setVisible(true);
        allAuctionsSection.setManaged(true);
        searchFilterBar.setVisible(true);
        searchFilterBar.setManaged(true);

        // Apply filters
        applyFilters();
    }

    // ==================== FILTERING ====================

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String categoryValue = categoryFilter.getValue();

        filteredAuctions.setAll(
                workspace.getAuctions().stream()
                        .filter(a -> matchesSearch(a, searchText))
                        .filter(a -> matchesStatus(a, statusValue))
                        .filter(a -> matchesCategory(a, categoryValue))
                        .collect(Collectors.toList())
        );

        renderAllAuctions();
    }

    private boolean matchesSearch(Auction auction, String searchText) {
        if (searchText.isEmpty()) return true;
        String name = auction.getItem().getName().toLowerCase();
        String seller = auction.getSeller() != null ? auction.getSeller().getUsername().toLowerCase() : "";
        return name.contains(searchText) || seller.contains(searchText);
    }

    private boolean matchesStatus(Auction auction, String status) {
        if (status == null || status.equals("Tất cả")) return true;

        return switch (status) {
            case "Chưa bắt đầu" -> auction.getStatus() == AuctionStatus.OPEN;
            case "Đang diễn ra" -> auction.getStatus() == AuctionStatus.RUNNING;
            case "Đã kết thúc" -> auction.getStatus() == AuctionStatus.FINISHED;
            case "Đã thanh toán" -> auction.getStatus() == AuctionStatus.PAID;
            case "Đã hủy" -> auction.getStatus() == AuctionStatus.CANCELED;
            default -> true;
        };
    }

    private boolean matchesCategory(Auction auction, String category) {
        if (category == null || category.equals("Tất cả")) return true;
        return auction.getItem().getItemCategory().equalsIgnoreCase(category);
    }

    // ==================== RENDERING ====================

    private void renderLiveAuctions() {
        liveAuctionsContainer.getChildren().clear();

        List<Auction> liveList = filteredAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());

        liveCountLabel.setText(String.valueOf(liveList.size()));

        if (liveList.isEmpty()) {
            liveEmptyState.setVisible(true);
            liveEmptyState.setManaged(true);
            return;
        }

        liveEmptyState.setVisible(false);
        liveEmptyState.setManaged(false);

        // Horizontal layout for live auctions
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        for (Auction auction : liveList) {
            VBox card = createLiveAuctionCard(auction);
            row.getChildren().add(card);
        }

        // Add scrollable container if too many items
        if (liveList.size() > 3) {
            ScrollPane scrollPane = new ScrollPane(row);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setPannable(true);
            scrollPane.getStyleClass().add("live-scroll");
            liveAuctionsContainer.getChildren().add(scrollPane);
        } else {
            liveAuctionsContainer.getChildren().add(row);
        }
    }

    private void renderAllAuctions() {
        allAuctionsContainer.getChildren().clear();

        allCountLabel.setText(String.valueOf(filteredAuctions.size()));
        totalAuctionsLabel.setText("Tổng: " + filteredAuctions.size() + " phiên");

        if (filteredAuctions.isEmpty()) {
            allEmptyState.setVisible(true);
            allEmptyState.setManaged(true);
            return;
        }

        allEmptyState.setVisible(false);
        allEmptyState.setManaged(false);

        for (Auction auction : filteredAuctions) {
            VBox card = createAllAuctionCard(auction);
            allAuctionsContainer.getChildren().add(card);
        }
    }

    // ==================== CARD CREATION ====================

    private VBox createLiveAuctionCard(Auction auction) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.setPrefWidth(300);
        card.setMinWidth(280);
        card.getStyleClass().add("live-auction-card");

        // Header with status badge
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(8);
        header.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-padding: 12;");
        header.getStyleClass().add("card-header");

        Label statusBadge = new Label("ĐANG DIỄN RA");
        statusBadge.getStyleClass().add("status-badge-live");

        Label categoryBadge = new Label(auction.getItem().getItemCategory());
        categoryBadge.getStyleClass().add("category-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(statusBadge, categoryBadge, spacer);

        // Body
        VBox body = new VBox();
        body.setSpacing(10);
        body.setStyle("-fx-background-color: white; -fx-padding: 15;");

        // Item name
        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("auction-name");

        // Current price
        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER_LEFT);

        Label priceTitle = new Label("Giá hiện tại:");
        priceTitle.getStyleClass().add("price-label");

        Label priceValue = new Label(AuctionWorkspace.formatVnd(auction.getItem().getCurrentHighestBid()) + " đ");
        priceValue.getStyleClass().add("price-value");

        priceBox.getChildren().addAll(priceTitle, priceValue);

        // Top bidder
        HBox bidderBox = new HBox(5);
        bidderBox.setAlignment(Pos.CENTER_LEFT);

        Label bidderIcon = new Label("👤");
        Label bidderTitle = new Label("Người dẫn đầu:");
        bidderTitle.getStyleClass().add("bidder-label");

        String topBidderName = "Chưa có";
        if (auction.getWinningBid() != null) {
            topBidderName = auction.getWinningBid().getBidder().getUsername();
        }
        Label bidderValue = new Label(topBidderName);
        bidderValue.getStyleClass().add("bidder-value");

        bidderBox.getChildren().addAll(bidderIcon, bidderTitle, bidderValue);

        // Countdown
        HBox countdownBox = new HBox(5);
        countdownBox.setAlignment(Pos.CENTER_LEFT);

        Label countdownIcon = new Label("⏰");
        Label countdownValue = new Label(formatCountdown(auction.getItem().getEndTime()));
        countdownValue.getStyleClass().add("countdown-value");

        // Store reference for updates
        card.getProperties().put("countdownLabel", countdownValue);
        card.getProperties().put("auction", auction);

        countdownBox.getChildren().addAll(countdownIcon, countdownValue);

        // Bid count
        int bidCount = auction.getBidHistory().size();
        HBox bidCountBox = new HBox(5);
        bidCountBox.setAlignment(Pos.CENTER_LEFT);

        Label bidCountIcon = new Label("🔨");
        Label bidCountValue = new Label(bidCount + " lượt đặt giá");
        bidCountValue.getStyleClass().add("bid-count");

        bidCountBox.getChildren().addAll(bidCountIcon, bidCountValue);

        // Seller
        HBox sellerBox = new HBox(5);
        sellerBox.setAlignment(Pos.CENTER_LEFT);

        Label sellerLabel = new Label("Người tạo: " + (auction.getSeller() != null ? auction.getSeller().getUsername() : "N/A"));
        sellerLabel.getStyleClass().add("seller-label");

        sellerBox.getChildren().add(sellerLabel);

        body.getChildren().addAll(nameLabel, priceBox, bidderBox, countdownBox, bidCountBox, sellerBox);

        // Action button
        Button joinButton = new Button("Tham gia đấu giá");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.getStyleClass().add("join-button");
        joinButton.setOnAction(e -> openAuctionRoom(auction));

        VBox actionBox = new VBox();
        actionBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10 15 15 15;");
        actionBox.getChildren().add(joinButton);

        // Combine
        card.getChildren().addAll(header, body, actionBox);

        return card;
    }

    private VBox createAllAuctionCard(Auction auction) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("all-auction-card");

        // Status indicator bar
        HBox statusBar = new HBox();
        statusBar.setPrefHeight(4);

        String statusColor = switch (auction.getStatus()) {
            case RUNNING -> "#10b981";    // Green
            case OPEN -> "#f59e0b";        // Orange
            case FINISHED, PAID -> "#6b7280"; // Gray
            case CANCELED -> "#ef4444";   // Red
        };
        statusBar.setStyle("-fx-background-color: " + statusColor + ";");

        // Main content
        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-padding: 15;");

        // Left: Info
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        // Name and category
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.getStyleClass().add("auction-name");

        Label categoryLabel = new Label(auction.getItem().getItemCategory());
        categoryLabel.getStyleClass().add("category-tag");

        nameBox.getChildren().addAll(nameLabel, categoryLabel);

        // Seller and bid count
        HBox metaBox = new HBox(15);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Label sellerLabel = new Label("👤 " + (auction.getSeller() != null ? auction.getSeller().getUsername() : "N/A"));
        sellerLabel.getStyleClass().add("meta-label");

        Label bidCountLabel = new Label("🔨 " + auction.getBidHistory().size() + " lượt");
        bidCountLabel.getStyleClass().add("meta-label");

        // Top bidder
        String topBidder = "Chưa có";
        if (auction.getWinningBid() != null) {
            topBidder = auction.getWinningBid().getBidder().getUsername();
        }
        Label topBidderLabel = new Label("🏆 " + topBidder);
        topBidderLabel.getStyleClass().add("meta-label");

        metaBox.getChildren().addAll(sellerLabel, bidCountLabel, topBidderLabel);

        infoBox.getChildren().addAll(nameBox, metaBox);

        // Center: Price
        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setMinWidth(150);

        Label priceTitle = new Label("Giá hiện tại");
        priceTitle.getStyleClass().add("price-title");

        Label priceValue = new Label(AuctionWorkspace.formatVnd(auction.getItem().getCurrentHighestBid()) + " đ");
        priceValue.getStyleClass().add("price-main");

        priceBox.getChildren().addAll(priceTitle, priceValue);

        // Right: Status & Time
        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(120);

        Label statusLabel = new Label(getStatusText(auction.getStatus()));
        statusLabel.getStyleClass().add("status-tag-" + auction.getStatus().name().toLowerCase());

        // Countdown label for updates
        Label countdownLabel = new Label(formatCountdown(auction.getItem().getEndTime()));
        countdownLabel.getStyleClass().add("countdown-label");
        countdownLabel.setAlignment(Pos.CENTER_RIGHT);

        // Store reference for updates
        card.getProperties().put("countdownLabel", countdownLabel);
        card.getProperties().put("auction", auction);

        rightBox.getChildren().addAll(statusLabel, countdownLabel);

        content.getChildren().addAll(infoBox, priceBox, rightBox);

        // Action button
        HBox actionBox = new HBox(8);
        actionBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10 15 10 15;");

        Button viewButton = new Button("Xem chi tiết");
        viewButton.getStyleClass().add("view-button");
        viewButton.setOnAction(e -> openAuctionRoom(auction));

        // Admin Delete Button
        Button deleteBtn = new Button("🗑 Xóa");
        deleteBtn.getStyleClass().add("action-btn-delete-admin");
        deleteBtn.setOnAction(e -> deleteAuctionAsAdmin(auction));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        actionBox.getChildren().add(viewButton);

        // Chỉ hiển thị nút xóa cho admin
        if (isAdmin) {
            actionBox.getChildren().add(deleteBtn);
        }

        actionBox.getChildren().add(spacer);

        card.getChildren().addAll(statusBar, content, actionBox);

        return card;
    }

    private void deleteAuctionAsAdmin(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa phiên đấu giá");
        confirm.setHeaderText("Bạn có chắc muốn xóa phiên này?");
        confirm.setContentText("Phiên: \"" + auction.getItem().getName() + "\"\n" +
                "Người tạo: " + (auction.getSeller() != null ? auction.getSeller().getUsername() : "N/A") + "\n" +
                "Hành động này không thể hoàn tác!");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) {
            return;
        }

        boolean success = workspace.deleteAuction(auction,
                SessionManager.getInstance().getUsername(), true);

        if (success) {
            applyFilters();
            refreshAuctionCards();
        }
    }

    // ==================== UTILITY METHODS ====================

    private String getStatusText(AuctionStatus status) {
        return switch (status) {
            case OPEN -> "Chưa bắt đầu";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }

    private String formatCountdown(LocalDateTime endTime) {
        if (endTime == null) {
            return "Không giới hạn";
        }
        java.time.Duration d =
                java.time.Duration.between(LocalDateTime.now(), endTime);
        if (d.isNegative() || d.isZero()) {
            return "Đã kết thúc";
        }
        long totalSeconds = d.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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

    // ==================== NAVIGATION ====================

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
    private void handleShowBidHistory() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/BidHistory.fxml")
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openAuctionRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/auctionsystem/Views/fxml/AuctionRoom.fxml"
                    )
            );

            Parent root = loader.load();

            AuctionRoomController controller = loader.getController();
            controller.setAuction(auction);

            Stage stage = new Stage();
            stage.setTitle(auction.getItem().getName());
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
            stage.setTitle("Thông Tin Cá Nhân");
            stage.setScene(new Scene(root));
            stage.initOwner(topUsernameLabel.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh balance after closing profile
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
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/Login.fxml")
            );
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng Nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
