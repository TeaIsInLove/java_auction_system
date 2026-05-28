package com.example.bai_tap_lon.model.auction;

import com.example.bai_tap_lon.exception.AuctionClosedException;
import com.example.bai_tap_lon.exception.AuctionException;
import com.example.bai_tap_lon.exception.InvalidBidException;
import com.example.bai_tap_lon.exception.SellerBiddingOwnItemException;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.Entity;
import com.example.bai_tap_lon.model.entity.item.Item;
import com.example.bai_tap_lon.model.entity.user.Seller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// Auction kế thừa Entity và implement AuctionSubject để phát thông báo
public class Auction extends Entity implements AuctionSubject {
    private Item item;
    private Seller seller;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory;
    private BidTransaction winningBid;

    // Danh sách những người đang "xem" phiên đấu giá này (Giao diện JavaFX hoặc Client Socket)
    private List<AuctionObserver> observers;

    // Sử dụng ReentrantLock để khóa luồng, bảo vệ dữ liệu khi nhiều người đặt giá cùng lúc
    private final ReentrantLock lock;

    public Auction(Item item, Seller seller) {
        super();
        this.item = item;
        this.seller = seller;
        this.status = AuctionStatus.OPEN; // Mặc định là OPEN [cite: 68]
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.lock = new ReentrantLock();
    }

    // --- LOGIC CHUYỂN TRẠNG THÁI ---
    public void startAuction() {
        if (this.status == AuctionStatus.OPEN) {
            this.status = AuctionStatus.RUNNING;
            System.out.println("Phiên đấu giá bắt đầu!");
        }
    }

    public void endAuction() {
        if (winningBid != null) {
            this.status = AuctionStatus.FINISHED; // Có người thắng nhưng chưa thanh toán
            System.out.println("Phiên đấu giá kết thúc. Người thắng: " + winningBid.getBidder().getUsername()
                    + " với giá: $" + winningBid.getBidAmount());
        } else {
            this.status = AuctionStatus.CANCELED; // Không ai mua
            System.out.println("Phiên đấu giá bị hủy do không có người trả giá.");
        }
    }

    /**
     * Đặt phiên vào trạng thái chờ duyệt (gọi khi tạo phiên qua UI, trước khi admin duyệt).
     */
    public void setPendingApproval() {
        this.status = AuctionStatus.PENDING_APPROVAL;
    }

    /**
     * Admin duyệt phiên: chuyển từ PENDING_APPROVAL → OPEN.
     */
    public void approveAuction() {
        if (this.status == AuctionStatus.PENDING_APPROVAL) {
            this.status = AuctionStatus.OPEN;
            System.out.println("Phiên đấu giá đã được duyệt: " + item.getName());
        }
    }

    /**
     * Hủy phiên đấu giá (chỉ khi chưa có ai bid)
     */
    public void cancelAuction() {
        if (bidHistory.isEmpty()) {
            this.status = AuctionStatus.CANCELED;
            System.out.println("Phiên đấu giá đã bị hủy: " + item.getName());
        }
    }

    /**
     * Đánh dấu phiên đấu giá là đã thanh toán
     */
    public void markAsPaid() {
        this.status = AuctionStatus.PAID;
        System.out.println("Phiên đấu giá đã được thanh toán: " + item.getName());
    }

    /** Khôi phục trạng thái khi đọc từ CSDL (không dùng cho luồng đấu giá trực tiếp). */
    public void restorePersistedState(AuctionStatus persistedStatus) {
        this.status = persistedStatus;
    }

    // --- LOGIC ĐA LUỒNG: ĐẶT GIÁ ---
    public void placeBid(BidTransaction newBid) throws AuctionException {
        // Dùng khóa Lock để đảm bảo tại 1 mili-giây, chỉ có 1 người được chạy đoạn code xét giá này
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá chưa mở hoặc đã kết thúc!");
            }

            if (seller != null && newBid.getBidder().getUsername().equalsIgnoreCase(seller.getUsername())) {
                throw new SellerBiddingOwnItemException("Người tạo phiên không được tự đặt giá vào phiên của mình!");
            }

            if (newBid.getBidAmount() <= item.getCurrentHighestBid()) {
                throw new InvalidBidException("Giá đặt phải cao hơn mức giá cao nhất hiện tại: "
                        + item.getCurrentHighestBid());
            }

            // Nếu hợp lệ: Cập nhật hệ thống
            bidHistory.add(newBid);
            winningBid = newBid;
            item.setCurrentHighestBid(newBid.getBidAmount());

            // ANTI-SNIPE: nếu bid trong vòng 5 phút cuối, gia hạn thêm 5 phút
            LocalDateTime endTime = item.getEndTime();
            if (endTime != null) {
                long minutesLeft = Duration.between(LocalDateTime.now(), endTime).toMinutes();
                if (minutesLeft >= 0 && minutesLeft < 5) {
                    item.setEndTime(endTime.plusMinutes(5));
                    notifyObservers(newBid, "Anti-snipe: gia han them 5 phut!");
                }
            }

            // THÔNG BÁO CHO TẤT CẢ GIAO DIỆN CẬP NHẬT REALTIME
            notifyObservers(newBid, "Có giá mới: $" + newBid.getBidAmount());

        } finally {
            // Luôn phải mở khóa trong khối finally để dù có lỗi xảy ra thì hệ thống không bị treo (Deadlock)
            lock.unlock();
        }
    }

    /**
     * Thêm bid từ database (không thông báo observers vì đây là dữ liệu cũ đã load).
     */
    public void addBidFromDatabase(BidTransaction bid) {
        bidHistory.add(bid);
        if (winningBid == null || bid.getBidAmount() > winningBid.getBidAmount()) {
            winningBid = bid;
        }
    }

    // --- CÁC HÀM CỦA OBSERVER PATTERN ---
    @Override
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(BidTransaction newBid, String message) {
        for (AuctionObserver obs : observers) {
            obs.update(this, newBid, message);
        }
    }

    // Getters...
    public Item getItem() { return item; }
    public Seller getSeller() { return seller; }
    public AuctionStatus getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }
    public BidTransaction getWinningBid() { return winningBid; }
    // --- HÀM KẾ THỪA TỪ ENTITY ---
    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===");
        System.out.println("ID Phiên: " + this.getId());
        if (seller != null) {
            System.out.println("Người bán: " + seller.getUsername());
        }
        System.out.println("Trạng thái: " + this.status);
        System.out.println("Tổng số lượt ra giá: " + bidHistory.size());
        if (winningBid != null) {
            System.out.println("Người dẫn đầu: " + winningBid.getBidder().getUsername()
                    + " ($" + winningBid.getBidAmount() + ")");
        }
        System.out.println("===============================");
    }
}