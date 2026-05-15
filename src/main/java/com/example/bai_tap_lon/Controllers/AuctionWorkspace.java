package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AuctionRepository;
import com.example.bai_tap_lon.auth.BidRepository;
import com.example.bai_tap_lon.auth.DatabaseManager;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionManager;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.item.Item;
import com.example.bai_tap_lon.model.entity.item.ItemFactory;
import com.example.bai_tap_lon.model.entity.user.Bidder;
import com.example.bai_tap_lon.model.entity.user.Seller;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class AuctionWorkspace {
    private static final AuctionWorkspace INSTANCE = new AuctionWorkspace();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final AuctionRepository auctionRepository = new AuctionRepository(databaseManager);
    private final BidRepository bidRepository = new BidRepository(databaseManager);
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();
    private final ObjectProperty<Auction> selectedAuction = new SimpleObjectProperty<>();
    private final ObservableList<String> activityLogs = FXCollections.observableArrayList();
    private final StringProperty message = new SimpleStringProperty("San sang");
    private final BooleanProperty successMessage = new SimpleBooleanProperty(true);
    private final LongProperty revision = new SimpleLongProperty();

    private AuctionWorkspace() {
    }

    public static AuctionWorkspace getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (auctions.isEmpty()) {
            if (auctionManager.getActiveAuctions().isEmpty()) {
                loadAuctionsFromDatabase();
            } else {
                syncFromManager();
            }
        }

        if (selectedAuction.get() == null && !auctions.isEmpty()) {
            selectedAuction.set(auctions.getFirst());
        }
    }

    private void loadAuctionsFromDatabase() {
        auctionManager.getActiveAuctions().clear();
        activityLogs.clear();

        List<Auction> loaded = auctionRepository.findAll();
        for (Auction auction : loaded) {
            auctionManager.addAuction(auction);
        }
        syncFromManager();

        if (auctions.isEmpty()) {
            appendLog("Chua co phien dau gia trong co so du lieu.");
            showMessage("Chua co phien dau gia. Hay tao phien moi.", true);
        } else {
            appendLog("Da tai " + auctions.size() + " phien tu co so du lieu.");
            showMessage("Da tai du lieu phien dau gia.", true);
        }
        touch();
    }

    public Auction createAuction(String type, String itemName, String description, double startingPrice,
                                 LocalDateTime startTime, LocalDateTime endTime,
                                 String sellerName, String extraInfo) {
        Auction auction = buildAuction(type, itemName, description, startingPrice, startTime, endTime, sellerName, extraInfo);
        auctionManager.addAuction(auction);
        auctions.add(auction);
        auctionRepository.insert(auction);
        selectedAuction.set(auction);
        appendLog("Tao phien moi: " + itemName + " - " + money(startingPrice));
        showMessage("Da tao phien dau gia moi.", true);
        touch();
        return auction;
    }

    /** Người dùng hiện tại có phải chủ phiên (người bán / người tạo) không. */
    public static boolean isAuctionSeller(Auction auction, String username) {
        if (auction == null || auction.getSeller() == null || username == null) {
            return false;
        }
        if (username.isBlank() || "Guest".equalsIgnoreCase(username.trim())) {
            return false;
        }
        return auction.getSeller().getUsername().trim().equalsIgnoreCase(username.trim());
    }

    public boolean startAuction(Auction auction, String actorUsername) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia trong bang.", false);
            return false;
        }
        if (!isAuctionSeller(auction, actorUsername)) {
            showMessage("Chi nguoi tao phien moi duoc bat dau phien dau gia.", false);
            return false;
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            showMessage("Chi phien OPEN moi co the bat dau.", false);
            return false;
        }

        auction.startAuction();
        auctionRepository.update(auction);
        appendLog("Bat dau phien: " + auction.getItem().getName());
        showMessage("Phien dau gia dang RUNNING.", true);
        touch();
        return true;
    }

    public boolean placeBid(Auction auction, String bidderName, double amount) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia trong bang.", false);
            return false;
        }
        if (bidderName == null || bidderName.trim().isEmpty()) {
            showMessage("Nhap ten nguoi dat gia.", false);
            return false;
        }
        if (isAuctionSeller(auction, bidderName.trim())) {
            showMessage("Nguoi tao phien khong duoc tu dat gia vao phien cua minh.", false);
            return false;
        }

        try {
            Bidder bidder = new Bidder(bidderName.trim(), "", emailFromName(bidderName), 0.0);
            BidTransaction bid = new BidTransaction(bidder, amount);
            auction.placeBid(bid);

            // Lưu bid vào database ngay lập tức
            bidRepository.saveBid(auction.getId(), bid);

            auctionRepository.update(auction);
            appendLog(bidder.getUsername() + " dat " + money(amount) + " cho " + auction.getItem().getName());
            showMessage("Da ghi nhan gia moi.", true);
            touch();
            return true;
        } catch (Exception ex) {
            showMessage(ex.getMessage(), false);
            return false;
        }
    }

    public boolean endAuction(Auction auction, String actorUsername) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia trong bang.", false);
            return false;
        }
        if (!isAuctionSeller(auction, actorUsername)) {
            showMessage("Chi nguoi tao phien moi duoc ket thuc phien dau gia.", false);
            return false;
        }
        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.CANCELED
                || auction.getStatus() == AuctionStatus.PAID) {
            showMessage("Phien nay da ket thuc.", false);
            return false;
        }

        auction.endAuction();
        auctionRepository.update(auction);
        appendLog("Ket thuc phien " + auction.getItem().getName() + " voi trang thai " + auction.getStatus());
        showMessage("Da ket thuc phien dau gia.", true);
        touch();
        return true;
    }

    public boolean deleteAuction(Auction auction, String username, boolean isAdmin) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia de xoa.", false);
            return false;
        }
        if (!isAdmin && !isAuctionSeller(auction, username)) {
            showMessage("Chi admin hoac nguoi tao phien moi duoc xoa.", false);
            return false;
        }

        auctionManager.removeAuction(auction);
        auctions.remove(auction);
        auctionRepository.delete(auction.getId());
        appendLog("Da xoa phien: " + auction.getItem().getName());
        showMessage("Da xoa phien dau gia.", true);
        touch();
        return true;
    }

    public void clearLogs() {
        activityLogs.clear();
        appendLog("Da xoa nhat ky.");
    }

    public ObservableList<Auction> getAuctions() {
        return auctions;
    }

    public ObjectProperty<Auction> selectedAuctionProperty() {
        return selectedAuction;
    }

    public Auction getSelectedAuction() {
        return selectedAuction.get();
    }

    public ObservableList<String> getActivityLogs() {
        return activityLogs;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public BooleanProperty successMessageProperty() {
        return successMessage;
    }

    public LongProperty revisionProperty() {
        return revision;
    }

    public void showMessage(String text, boolean success) {
        message.set(text == null ? "" : text);
        successMessage.set(success);
    }

    public static String money(double value) {
        return "$" + String.format(Locale.US, "%,.2f", value);
    }

    /** Hiển thị số tiền VNĐ (không dùng ký hiệu khoa học), nhóm nghìn bằng dấu chấm. */
    public static String formatVnd(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.##", symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(0);
        return df.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    public static String shortId(Auction auction) {
        if (auction == null || auction.getId() == null) {
            return "";
        }
        return auction.getId().length() <= 8 ? auction.getId() : auction.getId().substring(0, 8);
    }

    private void syncFromManager() {
        auctions.setAll(auctionManager.getActiveAuctions());
    }

    private Auction buildAuction(String type, String itemName, String description, double startingPrice,
                                 LocalDateTime startTime, LocalDateTime endTime,
                                 String sellerName, String extraInfo) {
        Item item = ItemFactory.createItem(type, itemName, description, startingPrice, startTime, endTime, extraInfo);
        Seller seller = new Seller(sellerName, "", emailFromName(sellerName), sellerName);
        return new Auction(item, seller);
    }

    private String emailFromName(String name) {
        String safeName = name == null ? "user" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".");
        if (safeName.isBlank()) {
            safeName = "user";
        }
        return safeName + "@local";
    }

    private void appendLog(String text) {
        activityLogs.add(LocalTime.now().format(LOG_TIME_FORMATTER) + "  " + text);
    }

    private void touch() {
        revision.set(revision.get() + 1);
    }
}
