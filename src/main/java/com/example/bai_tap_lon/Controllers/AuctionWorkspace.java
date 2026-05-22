package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.*;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionManager;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.PaymentTransaction;
import com.example.bai_tap_lon.model.entity.item.Item;
import com.example.bai_tap_lon.model.entity.item.ItemFactory;
import com.example.bai_tap_lon.model.entity.user.Bidder;
import com.example.bai_tap_lon.model.entity.user.Seller;
import com.example.bai_tap_lon.network.AuctionClient;
import com.example.bai_tap_lon.network.NetworkMessage;
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
    private final UserRepository userRepository = new UserRepository(databaseManager);
    private final PaymentRepository paymentRepository = new PaymentRepository(databaseManager);
    private final ObservableList<Auction> auctions = FXCollections.observableArrayList();
    private final ObjectProperty<Auction> selectedAuction = new SimpleObjectProperty<>();
    private final ObservableList<String> activityLogs = FXCollections.observableArrayList();
    private final StringProperty message = new SimpleStringProperty("San sang");
    private final BooleanProperty successMessage = new SimpleBooleanProperty(true);
    private final LongProperty revision = new SimpleLongProperty();

    private AuctionWorkspace() {
        // Khởi tạo bảng payment_transactions nếu chưa có
        paymentRepository.initializeTable();
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
        AuctionClient.getInstance().send(
                new NetworkMessage(NetworkMessage.Type.AUCTION_CREATED, auction.getId(), sellerName, startingPrice, itemName));
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

    /**
     * Edit the details of an OPEN auction (before it starts).
     * Starting price can only be changed if no bids have been placed yet.
     */
    public boolean updateAuctionDetails(Auction auction, String newName, String newDescription,
                                        double newStartingPrice, java.time.LocalDateTime newEndTime,
                                        String actorUsername) {
        if (auction == null) {
            showMessage("Không tìm thấy phiên đấu giá.", false);
            return false;
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            showMessage("Chỉ có thể chỉnh sửa phiên chưa bắt đầu (OPEN).", false);
            return false;
        }
        if (!isAuctionSeller(auction, actorUsername) && !actorUsername.equalsIgnoreCase("admin")) {
            showMessage("Chỉ người tạo phiên hoặc admin mới được chỉnh sửa.", false);
            return false;
        }
        if (newName == null || newName.isBlank()) {
            showMessage("Tên sản phẩm không được để trống.", false);
            return false;
        }
        if (newStartingPrice <= 0) {
            showMessage("Giá khởi điểm phải lớn hơn 0.", false);
            return false;
        }

        auction.getItem().setName(newName.trim());
        auction.getItem().setDescription(newDescription == null ? "" : newDescription.trim());

        // Only update starting price if no bids yet
        if (auction.getBidHistory().isEmpty()) {
            auction.getItem().setStartingPrice(newStartingPrice);
            auction.getItem().setCurrentHighestBid(newStartingPrice);
        }

        if (newEndTime != null && newEndTime.isAfter(auction.getItem().getStartTime())) {
            auction.getItem().setEndTime(newEndTime);
        }

        auctionRepository.update(auction);
        appendLog("Da chinh sua phien: " + newName);
        showMessage("Đã cập nhật phiên đấu giá.", true);
        touch();
        return true;
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
        AuctionClient.getInstance().send(
                new NetworkMessage(NetworkMessage.Type.AUCTION_STARTED, auction.getId(), actorUsername, 0, ""));
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

        String bidderEmail = emailFromName(bidderName.trim());

        // KIỂM TRA SỐ DƯ TRƯỚC KHI CHO PHÉP BID
        // Chỉ kiểm tra nếu user đã tồn tại trong database
        double currentBalance = getUserBalanceByEmail(bidderEmail);
        if (currentBalance > 0 && currentBalance < amount) {
            String msg = String.format("So du khong du! Can: %s, Ban co: %s", money(amount), money(currentBalance));
            showMessage(msg, false);
            appendLog("Loi: " + bidderName + " khong du tien de dat gia " + money(amount));
            return false;
        }

        try {
            Bidder bidder = new Bidder(bidderName.trim(), "", bidderEmail, currentBalance);
            BidTransaction bid = new BidTransaction(bidder, amount);
            auction.placeBid(bid);

            // Lưu bid vào database ngay lập tức
            bidRepository.saveBid(auction.getId(), bid);

            auctionRepository.update(auction);
            appendLog(bidder.getUsername() + " dat " + money(amount) + " cho " + auction.getItem().getName());
            showMessage("Da ghi nhan gia moi: " + money(amount), true);
            touch();
            AuctionClient.getInstance().send(
                    new NetworkMessage(NetworkMessage.Type.BID_PLACED, auction.getId(), bidderName, amount, ""));

            // Trigger auto-bids from other registered bidders
            AutoBidManager.getInstance().triggerAutoBids(auction, bidderName.trim(), this);
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
        // Chỉ kiểm tra CANCELED vì FINISHED không còn được dùng nữa (dùng PAID)
        if (auction.getStatus() == AuctionStatus.PAID
                || auction.getStatus() == AuctionStatus.CANCELED) {
            showMessage("Phien nay da ket thuc.", false);
            return false;
        }
        // Không cho phép kết thúc phiên đang ở trạng thái OPEN (chưa bắt đầu)
        if (auction.getStatus() == AuctionStatus.OPEN) {
            showMessage("Phien chua bat dau. Vui long bat dau phien truoc.", false);
            return false;
        }

        // Lấy thông tin người thắng và số tiền trước khi kết thúc
        BidTransaction winningBid = auction.getWinningBid();
        String itemName = auction.getItem().getName();
        String sellerName = auction.getSeller().getUsername();

        // Kết thúc phiên đấu giá
        auction.endAuction();
        auctionRepository.update(auction);
        appendLog("========================================");
        appendLog("KET THUC PHIEN: " + itemName);
        appendLog("Nguoi ban: " + sellerName);
        appendLog("========================================");

        // Xử lý thanh toán nếu có người thắng
        if (winningBid != null) {
            double bidAmount = winningBid.getBidAmount();
            String winnerName = winningBid.getBidder().getUsername();
            String winnerEmail = winningBid.getBidder().getEmail();

            boolean success = processPayment(
                auction.getId(), itemName,
                winnerName, winnerEmail,
                sellerName, auction.getSeller().getEmail(),
                bidAmount
            );

            if (success) {
                appendLog("----------------------------------------");
                appendLog("PHIEN DA KET THUC THANH CONG!");
                appendLog("Nguoi thang: " + winnerName);
                appendLog("So tien: " + money(bidAmount));
                appendLog("----------------------------------------");
                showMessage("Thanh toan thanh cong! " + winnerName + " tra " + money(bidAmount) + " cho " + sellerName, true);
            } else {
                appendLog("----------------------------------------");
                appendLog("PHIEN KET THUC - LOI THANH TOAN!");
                appendLog("----------------------------------------");
            }
        } else {
            appendLog("----------------------------------------");
            appendLog("PHIEN KET THUC - KHONG CO NGUOI THANG");
            appendLog("----------------------------------------");
            showMessage("Phien dau gia ket thuc. Khong co nguoi thang cuoc.", true);
        }

        appendLog("Trang thai: " + auction.getStatus());
        touch();
        AuctionClient.getInstance().send(
                new NetworkMessage(NetworkMessage.Type.AUCTION_ENDED, auction.getId(), actorUsername, 0, ""));
        return true;
    }

    /**
     * Xử lý thanh toán: Trừ tiền người mua, cộng tiền người bán, lưu transaction
     * @return true nếu thanh toán thành công, false nếu thất bại
     */
    private boolean processPayment(String auctionId, String auctionName,
                                String buyerName, String buyerEmail,
                                String sellerName, String sellerEmail,
                                double amount) {
        try {
            appendLog("Bat dau xu ly thanh toan...");
            appendLog("So tien: " + money(amount));

            // 1. Lấy số dư hiện tại của người mua từ database (tìm bằng username)
            double buyerBalance = userRepository.findByUsername(buyerName)
                    .map(AppUser::getBalance)
                    .orElse(0.0);
            boolean buyerExists = userRepository.findByUsername(buyerName).isPresent();
            appendLog("So du nguoi mua (" + buyerName + "): " + money(buyerBalance));

            // 2. Kiểm tra người mua có đủ tiền không
            // Nếu buyer chưa có trong DB (số dư = 0), coi như có đủ tiền (trường hợp test)
            if (buyerExists && buyerBalance < amount) {
                appendLog("LOI: Nguoi mua khong du tien!");
                appendLog("Can: " + money(amount) + " | Co: " + money(buyerBalance));
                showMessage("Nguoi thang cuoc khong du tien trong tai khoan!", false);
                return false;
            }

            // 3. Trừ tiền người mua (chỉ trừ nếu có trong database)
            double newBuyerBalance = buyerBalance;
            if (buyerExists) {
                newBuyerBalance = buyerBalance - amount;
                userRepository.updateBalance(buyerName, newBuyerBalance);
                appendLog("Da tru tien nguoi mua: " + money(amount));
                appendLog("So du con lai (" + buyerName + "): " + money(newBuyerBalance));
            } else {
                appendLog("Nguoi mua chua co trong DB - khong tru tien");
            }

            // 4. Lấy số dư hiện tại của người bán (tìm bằng username)
            double sellerBalance = userRepository.findByUsername(sellerName)
                    .map(AppUser::getBalance)
                    .orElse(0.0);
            boolean sellerExists = userRepository.findByUsername(sellerName).isPresent();
            appendLog("So du nguoi ban (" + sellerName + "): " + money(sellerBalance));

            // 5. Cộng tiền người bán (chỉ cộng nếu có trong database)
            double newSellerBalance = sellerBalance;
            if (sellerExists) {
                newSellerBalance = sellerBalance + amount;
                userRepository.updateBalance(sellerName, newSellerBalance);
                appendLog("Da cong tien nguoi ban: " + money(amount));
                appendLog("So du moi (" + sellerName + "): " + money(newSellerBalance));
            } else {
                appendLog("Nguoi ban chua co trong DB - khong cong tien");
            }

            // 6. Tạo và lưu payment transaction
            PaymentTransaction transaction = new PaymentTransaction(
                    auctionId, auctionName,
                    buyerName, buyerEmail,
                    sellerName, sellerEmail,
                    amount
            );
            paymentRepository.save(transaction);
            appendLog("Da luu giao dich thanh toan");

            System.out.println("===========================================");
            System.out.println("THANH TOAN THANH CONG!");
            System.out.println("  Nguoi mua: " + buyerName);
            if (buyerExists) {
                System.out.println("  Bi tru: " + money(amount));
                System.out.println("  So du con: " + money(newBuyerBalance));
            } else {
                System.out.println("  (Khong tru tien - chua co trong DB)");
            }
            System.out.println("  ---");
            System.out.println("  Nguoi ban: " + sellerName);
            System.out.println("  Duoc cong: " + money(amount));
            System.out.println("  So du moi: " + money(newSellerBalance));
            System.out.println("===========================================");

            return true;

        } catch (Exception ex) {
            appendLog("LOI: " + ex.getMessage());
            System.err.println("Loi xu ly thanh toan: " + ex.getMessage());
            ex.printStackTrace();
            showMessage("Da xay ra loi khi xu ly thanh toan!", false);
            return false;
        }
    }

    /**
     * Lấy số dư của một user từ database
     */
    public double getUserBalance(String username) {
        try {
            return userRepository.findByUsername(username)
                    .map(AppUser::getBalance)
                    .orElse(0.0);
        } catch (Exception ex) {
            System.err.println("Loi khi lay so du: " + ex.getMessage());
            return 0.0;
        }
    }

    /**
     * Lấy số dư của một user từ email
     */
    public double getUserBalanceByEmail(String email) {
        try {
            return userRepository.findByEmail(email)
                    .map(AppUser::getBalance)
                    .orElse(0.0);
        } catch (Exception ex) {
            System.err.println("Loi khi lay so du: " + ex.getMessage());
            return 0.0;
        }
    }

    /**
     * Lấy lịch sử giao dịch thanh toán của một phiên đấu giá
     */
    public List<PaymentTransaction> getPaymentHistoryByAuction(String auctionId) {
        try {
            return paymentRepository.findByAuctionId(auctionId);
        } catch (Exception ex) {
            System.err.println("Loi khi lay lich su thanh toan: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy lịch sử giao dịch của một người dùng (với tư cách người mua)
     */
    public List<PaymentTransaction> getPaymentHistoryByBuyer(String buyerUsername) {
        try {
            return paymentRepository.findByBuyer(buyerUsername);
        } catch (Exception ex) {
            System.err.println("Loi khi lay lich su mua hang: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy lịch sử giao dịch của một người dùng (với tư cách người bán)
     */
    public List<PaymentTransaction> getPaymentHistoryBySeller(String sellerUsername) {
        try {
            return paymentRepository.findBySeller(sellerUsername);
        } catch (Exception ex) {
            System.err.println("Loi khi lay lich su ban hang: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy toàn bộ lịch sử giao dịch
     */
    public List<PaymentTransaction> getAllPaymentHistory() {
        try {
            return paymentRepository.findAll();
        } catch (Exception ex) {
            System.err.println("Loi khi lay tat ca lich su thanh toan: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy tổng số tiền đã kiếm được của một người bán
     */
    public double getTotalEarnings(String sellerUsername) {
        try {
            return paymentRepository.findBySeller(sellerUsername).stream()
                    .mapToDouble(PaymentTransaction::getAmount)
                    .sum();
        } catch (Exception ex) {
            System.err.println("Loi khi tinh tong thu nhap: " + ex.getMessage());
            return 0.0;
        }
    }

    /**
     * Lấy tổng số tiền đã chi tiêu của một người mua
     */
    public double getTotalSpending(String buyerUsername) {
        try {
            return paymentRepository.findByBuyer(buyerUsername).stream()
                    .mapToDouble(PaymentTransaction::getAmount)
                    .sum();
        } catch (Exception ex) {
            System.err.println("Loi khi tinh tong chi tieu: " + ex.getMessage());
            return 0.0;
        }
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

    /**
     * Hủy một phiên đấu giá (chỉ khi chưa có ai bid)
     */
    public boolean cancelAuction(String auctionId) {
        Auction auction = auctions.stream()
                .filter(a -> a.getId().equals(auctionId))
                .findFirst()
                .orElse(null);

        if (auction == null) {
            showMessage("Khong tim thay phien dau gia.", false);
            return false;
        }

        if (!auction.getBidHistory().isEmpty()) {
            showMessage("Khong the huy phien da co nguoi dat gia.", false);
            return false;
        }

        auction.cancelAuction();
        auctionRepository.update(auction);
        appendLog("Da huy phien: " + auction.getItem().getName());
        showMessage("Da huy phien dau gia.", true);
        touch();
        return true;
    }

    /**
     * Đánh dấu phiên đấu giá là đã thanh toán
     */
    public boolean markAsPaid(String auctionId) {
        Auction auction = auctions.stream()
                .filter(a -> a.getId().equals(auctionId))
                .findFirst()
                .orElse(null);

        if (auction == null) {
            showMessage("Khong tim thay phien dau gia.", false);
            return false;
        }

        if (auction.getStatus() != AuctionStatus.FINISHED) {
            showMessage("Chi phien da ket thuc moi co the danh dau thanh toan.", false);
            return false;
        }

        auction.markAsPaid();
        auctionRepository.update(auction);
        appendLog("Da xac nhan thanh toan cho phien: " + auction.getItem().getName());
        showMessage("Da xac nhan thanh toan.", true);
        touch();
        return true;
    }

    /**
     * Reload a single auction from the database and replace the stale in-memory object.
     * Call this when a remote client broadcasts a change so local state stays fresh.
     * @return the freshly loaded Auction, or null if not found
     */
    public Auction reloadAuctionFromDb(String auctionId) {
        Auction fresh = auctionRepository.findById(auctionId);
        if (fresh == null) return null;

        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getId().equals(auctionId)) {
                auctions.set(i, fresh);
                if (selectedAuction.get() != null && selectedAuction.get().getId().equals(auctionId)) {
                    selectedAuction.set(fresh);
                }
                auctionManager.getActiveAuctions().replaceAll(a -> a.getId().equals(auctionId) ? fresh : a);
                return fresh;
            }
        }
        // Auction not yet in local list — add it
        auctionManager.addAuction(fresh);
        auctions.add(fresh);
        return fresh;
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
        // Look up the real email from DB; fall back to generated address only when not found
        String sellerEmail = userRepository.findByUsername(sellerName)
                .map(AppUser::getEmail)
                .orElseGet(() -> emailFromName(sellerName));
        Seller seller = new Seller(sellerName, "", sellerEmail, sellerName);
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
