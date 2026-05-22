package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.network.AuctionClient;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuctionRoomController {

    private static final long BID_INCREMENT_VND = 500_000L;
    private static final DateTimeFormatter BID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML private Label titleLabel;
    @FXML private Label categoryLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private TableView<BidHistoryRow> bidTable;
    @FXML private TableColumn<BidHistoryRow, String> userColumn;
    @FXML private TableColumn<BidHistoryRow, String> amountColumn;
    @FXML private TableColumn<BidHistoryRow, String> timeColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidStatusColumn;
    @FXML private Label nextMinBidLabel;
    @FXML private Button startSessionButton;
    @FXML private Button endSessionButton;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Label bidIncrementLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label participantsLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Label sellerNameLabel;

    // Bid trend chart
    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    // Auto-bid controls
    @FXML private CheckBox autoBidCheckBox;
    @FXML private TextField autoBidMaxField;
    @FXML private Label autoBidStatusLabel;

    private Auction auction;
    private Timeline refreshTimeline;
    private boolean columnsConfigured;

    @FXML
    public void initialize() {
        if (bidTable != null) {
            bidTable.setPlaceholder(new Label("Chưa có lượt đặt giá"));
        }
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    /** Gọi sau khi gắn Scene (trước hoặc sau show) để bắt đầu làm mới định kỳ. */
    public void initializeView() {
        if (auction == null) {
            return;
        }
        ensureColumns();
        refreshAll();
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshDynamicParts()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();

        // Listen for real-time updates from other clients via the network layer.
        // Reload from DB first so we get the remote client's changes, not stale local state.
        AuctionClient.getInstance().setOnMessageReceived(msg -> {
            if (auction != null && auction.getId().equals(msg.getAuctionId())) {
                Platform.runLater(() -> {
                    Auction fresh = AuctionWorkspace.getInstance().reloadAuctionFromDb(msg.getAuctionId());
                    if (fresh != null) {
                        this.auction = fresh;
                    }
                    refreshAll();
                });
            }
        });

        // Restore auto-bid checkbox state if previously registered
        if (autoBidCheckBox != null && auction != null) {
            String me = SessionManager.getInstance().getUsername();
            boolean registered = AutoBidManager.getInstance().isRegistered(auction.getId(), me);
            autoBidCheckBox.setSelected(registered);
            if (registered) {
                double max = AutoBidManager.getInstance().getMaxPrice(auction.getId(), me);
                if (autoBidMaxField != null) autoBidMaxField.setText(String.valueOf((long) max));
                updateAutoBidStatusLabel(true, max);
            }
        }
    }

    public void dispose() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
        AuctionClient.getInstance().setOnMessageReceived(null);
    }

    @FXML
    private void handleStartSession() {
        if (auction == null) {
            return;
        }
        String actor = SessionManager.getInstance().getUsername();
        boolean ok = AuctionWorkspace.getInstance().startAuction(auction, actor);
        if (!ok) {
            alert("Không thể bắt đầu phiên. Chỉ người tạo phiên (chủ bán) được phép, hoặc trạng thái phiên chưa phù hợp.");
            return;
        }
        refreshAll();
    }

    @FXML
    private void handleEndSession() {
        if (auction == null) {
            return;
        }
        String actor = SessionManager.getInstance().getUsername();
        boolean ok = AuctionWorkspace.getInstance().endAuction(auction, actor);
        if (!ok) {
            alert("Không thể kết thúc phiên. Chỉ người tạo phiên (chủ bán) được phép, hoặc phiên đã kết thúc.");
            return;
        }
        refreshAll();
    }

    @FXML
    private void handlePlaceBid() {
        if (auction == null) {
            return;
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            alert("Chỉ có thể đặt giá khi phiên đang diễn ra (RUNNING).");
            return;
        }

        Double amount = parseMoney(bidAmountField.getText());
        if (amount == null || amount <= 0) {
            alert("Nhập số tiền hợp lệ (VNĐ).");
            return;
        }

        String bidder = SessionManager.getInstance().getUsername();
        if (bidder == null || bidder.isBlank() || "Guest".equalsIgnoreCase(bidder)) {
            alert("Vui lòng đăng nhập để đặt giá.");
            return;
        }

        AuctionWorkspace workspace = AuctionWorkspace.getInstance();
        boolean ok = workspace.placeBid(auction, bidder, amount);
        if (ok) {
            bidAmountField.clear();
            refreshAll();
        }
    }

    /**
     * Toggle auto-bid on or off for the current user in this auction.
     * When turned on the max price field value is read and registered.
     */
    @FXML
    private void handleAutoBidToggle() {
        if (auction == null || autoBidCheckBox == null) return;

        String me = SessionManager.getInstance().getUsername();
        if (me == null || me.isBlank() || "Guest".equalsIgnoreCase(me)) {
            alert("Vui lòng đăng nhập để sử dụng đặt giá tự động.");
            autoBidCheckBox.setSelected(false);
            return;
        }

        if (autoBidCheckBox.isSelected()) {
            // Enabling — read max price
            Double max = parseMoney(autoBidMaxField != null ? autoBidMaxField.getText() : "");
            if (max == null || max <= 0) {
                alert("Nhập giá tối đa hợp lệ trước khi bật đặt giá tự động.");
                autoBidCheckBox.setSelected(false);
                return;
            }
            double currentHighest = auction.getItem().getCurrentHighestBid();
            if (max <= currentHighest) {
                alert("Giá tối đa phải cao hơn giá hiện tại (" + AuctionWorkspace.formatVnd(currentHighest) + " đ).");
                autoBidCheckBox.setSelected(false);
                return;
            }
            AutoBidManager.getInstance().register(auction.getId(), me, max);
            updateAutoBidStatusLabel(true, max);
        } else {
            // Disabling
            AutoBidManager.getInstance().unregister(auction.getId(), me);
            updateAutoBidStatusLabel(false, 0);
        }
    }

    private void updateAutoBidStatusLabel(boolean active, double maxPrice) {
        if (autoBidStatusLabel == null) return;
        if (active) {
            autoBidStatusLabel.setText("Đang bật — tối đa: " + AuctionWorkspace.formatVnd(maxPrice) + " đ");
        } else {
            autoBidStatusLabel.setText("Đã tắt");
        }
    }

    private void ensureColumns() {
        if (columnsConfigured || userColumn == null) {
            return;
        }
        userColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().username()));
        amountColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().amountText()));
        timeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().timeText()));
        bidStatusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().status()));
        columnsConfigured = true;
    }

    private void refreshAll() {
        refreshStaticHeader();
        refreshBidTable();
        refreshChart();
        refreshDynamicParts();
    }

    private void refreshStaticHeader() {
        titleLabel.setText(auction.getItem().getName());
        categoryLabel.setText(auction.getItem().getItemCategory());
    }

    private void refreshDynamicParts() {
        if (auction == null) {
            return;
        }

        double current = auction.getItem().getCurrentHighestBid();
        currentPriceLabel.setText(AuctionWorkspace.formatVnd(current) + " đ");

        BidTransaction winning = auction.getWinningBid();
        if (winning != null) {
            leaderLabel.setText(winning.getBidder().getUsername());
        } else {
            leaderLabel.setText("Chưa có");
        }

        timerLabel.setText(formatCountdown(auction.getItem().getEndTime()));
        applyStatusBadge();

        long nextHint = Math.round(current) + BID_INCREMENT_VND;
        nextMinBidLabel.setText(AuctionWorkspace.formatVnd(nextHint) + " đ");

        bidIncrementLabel.setText("Bước giá: " + AuctionWorkspace.formatVnd(BID_INCREMENT_VND) + " đ");

        if (auction.getSeller() != null) {
            sellerNameLabel.setText("Người tạo: " + auction.getSeller().getUsername());
        } else {
            sellerNameLabel.setText("Người tạo: Không xác định");
        }

        List<BidTransaction> history = auction.getBidHistory();
        totalBidsLabel.setText("Tổng lượt bid: " + history.size());
        participantsLabel.setText("Người tham gia: " + countDistinctBidders(history));
        sessionStatusLabel.setText("Trạng thái: " + sessionStatusVietnamese(auction.getStatus()));

        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING;
        placeBidButton.setDisable(!canBid);

        boolean host = AuctionWorkspace.isAuctionSeller(auction, SessionManager.getInstance().getUsername());
        boolean showStart = host && auction.getStatus() == AuctionStatus.OPEN;
        boolean showEnd = host && (auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING);
        if (startSessionButton != null) {
            startSessionButton.setVisible(showStart);
            startSessionButton.setManaged(showStart);
        }
        if (endSessionButton != null) {
            endSessionButton.setVisible(showEnd);
            endSessionButton.setManaged(showEnd);
        }

        refreshBidTable();
    }

    private void refreshBidTable() {
        if (bidTable == null || auction == null) {
            return;
        }
        BidTransaction winning = auction.getWinningBid();
        List<BidTransaction> sorted = new ArrayList<>(auction.getBidHistory());
        sorted.sort(Comparator.comparing(BidTransaction::getBidTime).reversed());

        ObservableList<BidHistoryRow> rows = FXCollections.observableArrayList();
        for (BidTransaction tx : sorted) {
            boolean isLeading = winning != null && tx == winning;
            String status = isLeading ? "Dẫn đầu" : "Đã qua";
            rows.add(new BidHistoryRow(
                    tx.getBidder().getUsername(),
                    AuctionWorkspace.formatVnd(tx.getBidAmount()) + " đ",
                    tx.getBidTime().format(BID_TIME_FORMAT),
                    status
            ));
        }
        bidTable.setItems(rows);
    }

    /**
     * Refresh the bid price trend LineChart.
     * X-axis = bid number (chronological), Y-axis = price in millions VNĐ.
     */
    private void refreshChart() {
        if (bidChart == null || auction == null) return;

        List<BidTransaction> chronological = new ArrayList<>(auction.getBidHistory());
        chronological.sort(Comparator.comparing(BidTransaction::getBidTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Giá");

        for (int i = 0; i < chronological.size(); i++) {
            double priceMillions = chronological.get(i).getBidAmount() / 1_000_000.0;
            series.getData().add(new XYChart.Data<>(i + 1, priceMillions));
        }

        bidChart.getData().clear();
        if (!chronological.isEmpty()) {
            bidChart.getData().add(series);
        }
    }

    private static int countDistinctBidders(List<BidTransaction> history) {
        Set<String> names = new LinkedHashSet<>();
        for (BidTransaction tx : history) {
            names.add(tx.getBidder().getUsername());
        }
        return names.size();
    }

    private void applyStatusBadge() {
        statusBadgeLabel.getStyleClass().removeAll("live-badge", "open-badge", "ended-badge");
        AuctionStatus st = auction.getStatus();
        switch (st) {
            case RUNNING -> {
                statusBadgeLabel.getStyleClass().add("live-badge");
                statusBadgeLabel.setText("ĐANG DIỄN RA");
            }
            case OPEN -> {
                statusBadgeLabel.getStyleClass().add("open-badge");
                statusBadgeLabel.setText("CHƯA BẮT ĐẦU");
            }
            case FINISHED, PAID, CANCELED -> {
                statusBadgeLabel.getStyleClass().add("ended-badge");
                statusBadgeLabel.setText(switch (st) {
                    case FINISHED -> "ĐÃ KẾT THÚC";
                    case PAID -> "ĐÃ THANH TOÁN";
                    case CANCELED -> "ĐÃ HỦY";
                    default -> "KẾT THÚC";
                });
            }
        }
    }

    /** Nhãn tiếng Việt, khớp với badge (OPEN = chưa bắt đầu đặt giá). */
    private static String sessionStatusVietnamese(AuctionStatus st) {
        return switch (st) {
            case OPEN -> "Chưa bắt đầu";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã kết thúc";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }

    private String formatCountdown(LocalDateTime endTime) {
        if (endTime == null) {
            return "-- : -- : --";
        }
        java.time.Duration d = java.time.Duration.between(LocalDateTime.now(), endTime);
        if (d.isNegative() || d.isZero()) {
            return "00 : 00 : 00";
        }
        long totalSeconds = d.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d : %02d : %02d", hours, minutes, seconds);
    }

    private static Double parseMoney(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(raw.trim().replace(",", ""));
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    private static void alert(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    public Auction getAuction() {
        return auction;
    }

    public record BidHistoryRow(String username, String amountText, String timeText, String status) {}
}
