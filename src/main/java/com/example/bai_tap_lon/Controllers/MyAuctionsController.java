package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MyAuctionsController {

    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();
    private Timeline countdownTimer;

    // FXML Elements - Top Bar
    @FXML private Label topUsernameLabel;
    @FXML private Label balanceLabel;
    @FXML private Label sidebarUsernameLabel;

    // FXML Elements - Stats
    @FXML private Label totalSessionsLabel;
    @FXML private Label runningSessionsLabel;
    @FXML private Label completedSessionsLabel;
    @FXML private Label totalRevenueLabel;

    // FXML Elements - Tabs
    @FXML private TabPane tabPane;
    @FXML private VBox runningAuctionsContainer;
    @FXML private VBox finishedAuctionsContainer;
    @FXML private VBox emptyState;

    private String currentUsername;
    private boolean isAdmin;

    @FXML
    public void initialize() {
        workspace.initialize();

        currentUsername = SessionManager.getInstance().getUsername();
        isAdmin = SessionManager.getInstance().isAdmin();
        topUsernameLabel.setText(currentUsername);
        sidebarUsernameLabel.setText(currentUsername);

        loadBalance();
        loadStats();
        loadAuctions();
        setupListeners();
        startCountdownTimer();
    }

    private void setupListeners() {
        workspace.revisionProperty().addListener((obs, oldV, newV) -> {
            loadStats();
            loadAuctions();
            loadBalance();
        });
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdowns()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdowns() {
        Platform.runLater(() -> {
            updateCountdownInContainer(runningAuctionsContainer);
        });
    }

    private void updateCountdownInContainer(VBox container) {
        for (javafx.scene.Node node : container.getChildren()) {
            if (node instanceof VBox card) {
                Label countdownLabel = (Label) card.getProperties().get("countdownLabel");
                Auction auction = (Auction) card.getProperties().get("auction");
                if (countdownLabel != null && auction != null) {
                    countdownLabel.setText(formatCountdown(auction.getItem().getEndTime()));
                }
            }
        }
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

    private void loadStats() {
        List<Auction> myAuctions = getMyAuctions();

        long total = myAuctions.size();
        long running = myAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                          || a.getStatus() == AuctionStatus.OPEN
                          || a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .count();
        long completed = myAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID)
                .count();
        double revenue = myAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PAID)
                .mapToDouble(a -> a.getItem().getCurrentHighestBid())
                .sum();

        totalSessionsLabel.setText(String.valueOf(total));
        runningSessionsLabel.setText(String.valueOf(running));
        completedSessionsLabel.setText(String.valueOf(completed));
        totalRevenueLabel.setText(AuctionWorkspace.formatVnd(revenue) + " đ");
    }

    private void loadAuctions() {
        List<Auction> myAuctions = getMyAuctions();

        // Running auctions (including pending approval and open)
        List<Auction> runningList = myAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                          || a.getStatus() == AuctionStatus.OPEN
                          || a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .collect(Collectors.toList());

        // Finished auctions
        List<Auction> finishedList = myAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID || a.getStatus() == AuctionStatus.CANCELED)
                .collect(Collectors.toList());

        renderRunningAuctions(runningList);
        renderFinishedAuctions(finishedList);

        // Show/hide empty state
        if (myAuctions.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }
    }

    private List<Auction> getMyAuctions() {
        return workspace.getAuctions().stream()
                .filter(a -> a.getSeller() != null)
                .filter(a -> a.getSeller().getUsername().equals(currentUsername))
                .collect(Collectors.toList());
    }

    private void renderRunningAuctions(List<Auction> auctions) {
        runningAuctionsContainer.getChildren().clear();

        if (auctions.isEmpty()) {
            Label empty = new Label("Không có phiên nào đang diễn ra");
            empty.setStyle("-fx-text-fill: #5d6d7e; -fx-font-size: 14px; -fx-padding: 40;");
            empty.setAlignment(Pos.CENTER);
            runningAuctionsContainer.getChildren().add(empty);
            return;
        }

        for (Auction auction : auctions) {
            VBox card = createRunningAuctionCard(auction);
            runningAuctionsContainer.getChildren().add(card);
        }
    }

    private void renderFinishedAuctions(List<Auction> auctions) {
        finishedAuctionsContainer.getChildren().clear();

        if (auctions.isEmpty()) {
            Label empty = new Label("Không có phiên nào đã kết thúc");
            empty.setStyle("-fx-text-fill: #5d6d7e; -fx-font-size: 14px; -fx-padding: 40;");
            empty.setAlignment(Pos.CENTER);
            finishedAuctionsContainer.getChildren().add(empty);
            return;
        }

        for (Auction auction : auctions) {
            VBox card = createFinishedAuctionCard(auction);
            finishedAuctionsContainer.getChildren().add(card);
        }
    }

    private VBox createRunningAuctionCard(Auction auction) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("my-auction-card");

        // Status bar
        HBox statusBar = new HBox();
        statusBar.setPrefHeight(4);
        String statusColor = auction.getStatus() == AuctionStatus.RUNNING ? "#27ae60" : "#e67e22";
        statusBar.setStyle("-fx-background-color: " + statusColor + ";");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        header.setStyle("-fx-background-color: #faf8f5; -fx-padding: 15;");

        // Product info
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.getStyleClass().add("auction-name");

        Label categoryLabel = new Label(auction.getItem().getItemCategory());
        categoryLabel.getStyleClass().add("category-tag");

        HBox catBox = new HBox(8);
        catBox.getChildren().addAll(nameLabel, categoryLabel);

        String badgeText = switch (auction.getStatus()) {
            case RUNNING -> "ĐANG DIỄN RA";
            case PENDING_APPROVAL -> "CHỜ DUYỆT";
            default -> "SẮP BẮT ĐẦU";
        };
        String badgeStyle = switch (auction.getStatus()) {
            case RUNNING -> "status-tag-running";
            case PENDING_APPROVAL -> "status-tag-finished";
            default -> "status-tag-open";
        };
        Label statusBadge = new Label(badgeText);
        statusBadge.getStyleClass().add(badgeStyle);

        header.getChildren().addAll(infoBox, statusBadge);
        infoBox.getChildren().add(catBox);

        // Body
        VBox body = new VBox();
        body.setStyle("-fx-background-color: white; -fx-padding: 15;");
        body.setSpacing(12);

        // Price & Top Bidder row
        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // Current Price
        VBox priceBox = new VBox(2);
        Label priceTitle = new Label("Giá hiện tại");
        priceTitle.getStyleClass().add("price-title");
        Label priceValue = new Label(AuctionWorkspace.formatVnd(auction.getItem().getCurrentHighestBid()) + " đ");
        priceValue.getStyleClass().add("price-main");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        // Top Bidder
        VBox bidderBox = new VBox(2);
        Label bidderTitle = new Label("Người dẫn đầu");
        bidderTitle.getStyleClass().add("price-title");
        String topBidder = "Chưa có bid";
        if (auction.getWinningBid() != null) {
            topBidder = auction.getWinningBid().getBidder().getUsername();
        }
        Label bidderValue = new Label(topBidder);
        bidderValue.getStyleClass().add("bidder-value");
        bidderBox.getChildren().addAll(bidderTitle, bidderValue);

        // Time Left
        VBox timeBox = new VBox(2);
        Label timeTitle = new Label("Thời gian còn lại");
        timeTitle.getStyleClass().add("price-title");
        Label countdownLabel = new Label(formatCountdown(auction.getItem().getEndTime()));
        countdownLabel.getStyleClass().add("countdown-value");
        card.getProperties().put("countdownLabel", countdownLabel);
        card.getProperties().put("auction", auction);
        timeBox.getChildren().addAll(timeTitle, countdownLabel);

        // Bid Count
        VBox bidBox = new VBox(2);
        Label bidTitle = new Label("Lượt bid");
        bidTitle.getStyleClass().add("price-title");
        Label bidValue = new Label(String.valueOf(auction.getBidHistory().size()));
        bidValue.getStyleClass().add("bid-count");
        bidBox.getChildren().addAll(bidTitle, bidValue);

        mainRow.getChildren().addAll(priceBox, bidderBox, timeBox, bidBox);
        body.getChildren().add(mainRow);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-background-color: #f5f0e8; -fx-padding: 10 15;");

        Button viewBtn = new Button("👁 Xem chi tiết");
        viewBtn.getStyleClass().add("action-btn-view");

        Button editBtn = new Button("✏ Chỉnh sửa");
        editBtn.getStyleClass().add("action-btn-edit");

        // Cancel button - only if no bids
        Button cancelBtn = new Button("❌ Hủy phiên");
        cancelBtn.getStyleClass().add("action-btn-cancel");
        if (!auction.getBidHistory().isEmpty()) {
            cancelBtn.setDisable(true);
            cancelBtn.setTooltip(new Tooltip("Không thể hủy vì đã có người bid"));
        }

        // Pause button
        Button pauseBtn = new Button("⏸ Tạm dừng");
        pauseBtn.getStyleClass().add("action-btn-pause");
        pauseBtn.setOnAction(e -> togglePause(auction, pauseBtn));

        actions.getChildren().addAll(viewBtn, editBtn, pauseBtn);

        if (cancelBtn.isDisabled() == false) {
            actions.getChildren().add(cancelBtn);
        }

        card.getChildren().addAll(statusBar, header, body, actions);

        viewBtn.setOnAction(e -> openAuctionRoom(auction));
        editBtn.setOnAction(e -> handleEditAuction(auction));
        cancelBtn.setOnAction(e -> cancelAuction(auction));

        return card;
    }

    private void handleEditAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.OPEN) {
            new Alert(Alert.AlertType.WARNING,
                    "Chỉ có thể chỉnh sửa phiên ở trạng thái OPEN (chưa bắt đầu).").showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa phiên đấu giá");
        dialog.setHeaderText("Chỉnh sửa: " + auction.getItem().getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField nameField = new TextField(auction.getItem().getName());
        TextArea descArea = new TextArea(auction.getItem().getDescription());
        descArea.setPrefRowCount(3);

        boolean hasBids = !auction.getBidHistory().isEmpty();
        TextField priceField = new TextField(String.valueOf((long) auction.getItem().getStartingPrice()));
        priceField.setDisable(hasBids);
        if (hasBids) priceField.setTooltip(new Tooltip("Không thể thay đổi giá khi đã có bid"));

        DatePicker endDatePicker = new DatePicker(auction.getItem().getEndTime().toLocalDate());
        TextField endTimeField = new TextField(auction.getItem().getEndTime().toLocalTime()
                .withSecond(0).withNano(0).toString());

        grid.add(new Label("Tên sản phẩm:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Mô tả:"),         0, 1); grid.add(descArea,  1, 1);
        grid.add(new Label("Giá khởi điểm:"), 0, 2); grid.add(priceField, 1, 2);
        grid.add(new Label("Ngày kết thúc:"), 0, 3); grid.add(endDatePicker, 1, 3);
        grid.add(new Label("Giờ kết thúc:"),  0, 4); grid.add(endTimeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        double newPrice;
        try {
            newPrice = Double.parseDouble(priceField.getText().trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Giá khởi điểm không hợp lệ.").showAndWait();
            return;
        }

        LocalDateTime newEndTime;
        try {
            LocalTime time = LocalTime.parse(endTimeField.getText().trim());
            newEndTime = LocalDateTime.of(endDatePicker.getValue(), time);
        } catch (DateTimeParseException ex) {
            new Alert(Alert.AlertType.ERROR, "Giờ kết thúc không hợp lệ (dùng định dạng HH:mm).").showAndWait();
            return;
        }

        boolean ok = workspace.updateAuctionDetails(
                auction, nameField.getText(), descArea.getText(),
                newPrice, newEndTime, currentUsername);

        if (ok) loadAuctions();
    }

    private VBox createFinishedAuctionCard(Auction auction) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("my-auction-card");

        // Status bar
        HBox statusBar = new HBox();
        statusBar.setPrefHeight(4);
        String statusColor;
        String statusText;
        switch (auction.getStatus()) {
            case PAID -> { statusColor = "#27ae60"; statusText = "ĐÃ THANH TOÁN"; }
            case FINISHED -> { statusColor = "#5d6d7e"; statusText = "CHƯA THANH TOÁN"; }
            default -> { statusColor = "#e74c3c"; statusText = "ĐÃ HỦY"; }
        }
        statusBar.setStyle("-fx-background-color: " + statusColor + ";");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        header.setStyle("-fx-background-color: #faf8f5; -fx-padding: 15;");

        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.getStyleClass().add("auction-name");

        Label categoryLabel = new Label(auction.getItem().getItemCategory());
        categoryLabel.getStyleClass().add("category-tag");

        HBox catBox = new HBox(8);
        catBox.getChildren().addAll(nameLabel, categoryLabel);

        Label statusBadge = new Label(statusText);
        statusBadge.getStyleClass().add(auction.getStatus() == AuctionStatus.PAID ? "status-tag-paid" : "status-tag-finished");

        header.getChildren().addAll(infoBox, statusBadge);
        infoBox.getChildren().add(catBox);

        // Body
        VBox body = new VBox();
        body.setStyle("-fx-background-color: white; -fx-padding: 15;");
        body.setSpacing(12);

        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // Winner
        VBox winnerBox = new VBox(2);
        Label winnerTitle = new Label("Người thắng");
        winnerTitle.getStyleClass().add("price-title");
        String winner = auction.getWinningBid() != null ? auction.getWinningBid().getBidder().getUsername() : "Không có";
        Label winnerValue = new Label(winner);
        winnerValue.getStyleClass().add("winner-name");
        winnerBox.getChildren().addAll(winnerTitle, winnerValue);

        // Final Price
        VBox priceBox = new VBox(2);
        Label priceTitle = new Label("Giá cuối cùng");
        priceTitle.getStyleClass().add("price-title");
        Label priceValue = new Label(AuctionWorkspace.formatVnd(auction.getItem().getCurrentHighestBid()) + " đ");
        priceValue.getStyleClass().add("price-main");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        // End Time
        VBox timeBox = new VBox(2);
        Label timeTitle = new Label("Kết thúc lúc");
        timeTitle.getStyleClass().add("price-title");
        String endTimeStr = auction.getItem().getEndTime() != null ?
                auction.getItem().getEndTime().toLocalDate().toString() : "N/A";
        Label timeValue = new Label(endTimeStr);
        timeValue.getStyleClass().add("meta-label");
        timeBox.getChildren().addAll(timeTitle, timeValue);

        // Total Bids
        VBox bidBox = new VBox(2);
        Label bidTitle = new Label("Tổng lượt bid");
        bidTitle.getStyleClass().add("price-title");
        Label bidValue = new Label(String.valueOf(auction.getBidHistory().size()));
        bidValue.getStyleClass().add("bid-count");
        bidBox.getChildren().addAll(bidTitle, bidValue);

        mainRow.getChildren().addAll(winnerBox, priceBox, timeBox, bidBox);
        body.getChildren().add(mainRow);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-background-color: #f5f0e8; -fx-padding: 10 15;");

        Button viewBtn = new Button("📄 Xem kết quả");
        viewBtn.getStyleClass().add("action-btn-view");

        Button paymentBtn = new Button("💰 Xác nhận thanh toán");
        paymentBtn.getStyleClass().add("action-btn-payment");
        if (auction.getStatus() == AuctionStatus.PAID) {
            paymentBtn.setText("✅ Đã thanh toán");
            paymentBtn.setDisable(true);
        }

        Button relistBtn = new Button("🔄 Đăng lại phiên");
        relistBtn.getStyleClass().add("action-btn-relist");

        // Admin Delete Button
        Button deleteBtn = new Button("🗑 Xóa phiên");
        deleteBtn.getStyleClass().add("action-btn-delete-admin");

        actions.getChildren().addAll(viewBtn, paymentBtn, relistBtn);

        // Chỉ hiển thị nút xóa cho admin
        if (isAdmin) {
            actions.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(statusBar, header, body, actions);

        viewBtn.setOnAction(e -> openAuctionRoom(auction));
        paymentBtn.setOnAction(e -> confirmPayment(auction));
        relistBtn.setOnAction(e -> relistAuction(auction));
        deleteBtn.setOnAction(e -> deleteAuctionAsAdmin(auction));

        return card;
    }

    private void togglePause(Auction auction, Button pauseBtn) {
        // Toggle pause logic - placeholder
        System.out.println("Toggle pause for auction: " + auction.getItem().getName());
    }

    private void cancelAuction(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hủy");
        confirm.setHeaderText("Hủy phiên đấu giá?");
        confirm.setContentText("Bạn có chắc muốn hủy phiên \"" + auction.getItem().getName() + "\" không?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            workspace.cancelAuction(auction.getId());
            loadAuctions();
            loadStats();
        }
    }

    private void confirmPayment(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán");
        confirm.setHeaderText("Xác nhận đã nhận thanh toán?");
        confirm.setContentText("Xác nhận rằng bạn đã nhận được " +
                AuctionWorkspace.formatVnd(auction.getItem().getCurrentHighestBid()) + " đ từ người thắng.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            workspace.markAsPaid(auction.getId());
            loadAuctions();
            loadStats();
        }
    }

    private void deleteAuctionAsAdmin(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa phiên đấu giá");
        confirm.setHeaderText("Bạn có chắc muốn xóa phiên này?");
        confirm.setContentText("Phiên: \"" + auction.getItem().getName() + "\"\n" +
                "Hành động này không thể hoàn tác!");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            workspace.deleteAuction(auction, currentUsername, true);
            loadAuctions();
            loadStats();
        }
    }

    private void relistAuction(Auction auction) {
        // Open create auction dialog with pre-filled data
        handleOpenCreateAuction();
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
            stage.setOnHidden(e -> controller.dispose());
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
    private void handleShowBidHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/auctionsystem/Views/fxml/BidHistory.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) topUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể mở Lịch sử đấu giá:\n" + e.getMessage()).showAndWait();
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

            loadAuctions();
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

        if (hours >= 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
