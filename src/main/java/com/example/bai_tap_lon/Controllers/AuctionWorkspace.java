package com.example.bai_tap_lon.Controllers;

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
                loadSampleAuctions();
            } else {
                syncFromManager();
            }
        }

        if (selectedAuction.get() == null && !auctions.isEmpty()) {
            selectedAuction.set(auctions.getFirst());
        }
    }

    public void loadSampleAuctions() {
        auctionManager.getActiveAuctions().clear();
        activityLogs.clear();

        LocalDateTime now = LocalDateTime.now();
        Auction laptop = buildAuction(
                "electronics",
                "Laptop Gaming",
                "Laptop hieu nang cao",
                900.0,
                now.minusHours(2),
                now.plusDays(2),
                "Tech Store",
                "Acer"
        );
        laptop.startAuction();
        addBidSilently(laptop, "minh", 1050.0);
        addBidSilently(laptop, "linh", 1250.0);

        Auction painting = buildAuction(
                "art",
                "Tranh Son Dau",
                "Tac pham nghe thuat",
                450.0,
                now.plusHours(4),
                now.plusDays(5),
                "Gallery One",
                "Nguyen Artist"
        );

        Auction motorbike = buildAuction(
                "vehicle",
                "Xe May Classic",
                "Phuong tien suu tam",
                1800.0,
                now.minusDays(5),
                now.minusHours(1),
                "Auto House",
                "Honda"
        );
        motorbike.startAuction();
        addBidSilently(motorbike, "tuan", 2100.0);
        addBidSilently(motorbike, "ha", 2320.0);
        motorbike.endAuction();

        auctionManager.getActiveAuctions().addAll(List.of(laptop, painting, motorbike));
        syncFromManager();
        selectedAuction.set(auctions.getFirst());
        appendLog("San sang voi " + auctions.size() + " phien dau gia mau.");
        showMessage("Da tai du lieu mau.", true);
        touch();
    }

    public Auction createAuction(String type, String itemName, String description, double startingPrice,
                                 LocalDateTime startTime, LocalDateTime endTime,
                                 String sellerName, String extraInfo) {
        Auction auction = buildAuction(type, itemName, description, startingPrice, startTime, endTime, sellerName, extraInfo);
        auctionManager.addAuction(auction);
        auctions.add(auction);
        selectedAuction.set(auction);
        appendLog("Tao phien moi: " + itemName + " - " + money(startingPrice));
        showMessage("Da tao phien dau gia moi.", true);
        touch();
        return auction;
    }

    public void startAuction(Auction auction) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia trong bang.", false);
            return;
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            showMessage("Chi phien OPEN moi co the bat dau.", false);
            return;
        }

        auction.startAuction();
        appendLog("Bat dau phien: " + auction.getItem().getName());
        showMessage("Phien dau gia dang RUNNING.", true);
        touch();
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

        try {
            Bidder bidder = new Bidder(bidderName.trim(), "", emailFromName(bidderName), 0.0);
            auction.placeBid(new BidTransaction(bidder, amount));
            appendLog(bidder.getUsername() + " dat " + money(amount) + " cho " + auction.getItem().getName());
            showMessage("Da ghi nhan gia moi.", true);
            touch();
            return true;
        } catch (Exception ex) {
            showMessage(ex.getMessage(), false);
            return false;
        }
    }

    public void endAuction(Auction auction) {
        if (auction == null) {
            showMessage("Chon mot phien dau gia trong bang.", false);
            return;
        }
        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.CANCELED
                || auction.getStatus() == AuctionStatus.PAID) {
            showMessage("Phien nay da ket thuc.", false);
            return;
        }

        auction.endAuction();
        appendLog("Ket thuc phien " + auction.getItem().getName() + " voi trang thai " + auction.getStatus());
        showMessage("Da ket thuc phien dau gia.", true);
        touch();
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

    private void addBidSilently(Auction auction, String bidderName, double amount) {
        try {
            auction.placeBid(new BidTransaction(new Bidder(bidderName, "", emailFromName(bidderName), 0.0), amount));
        } catch (Exception ignored) {
            // Sample data is deterministic; ignore only keeps startup resilient if model rules change.
        }
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
