package com.example.bai_tap_lon.model;

import java.util.ArrayList;
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
        this.status = AuctionStatus.FINISHED;
        if (winningBid != null) {
            System.out.println("Phiên đấu giá kết thúc. Người thắng: " + winningBid.getBidder().getUsername());
        } else {
            this.status = AuctionStatus.CANCELED; // Không ai mua
            System.out.println("Phiên đấu giá bị hủy do không có người trả giá.");
        }
    }

    // --- LOGIC ĐA LUỒNG: ĐẶT GIÁ ---
    public void placeBid(BidTransaction newBid) throws Exception {
        // Dùng khóa Lock để đảm bảo tại 1 mili-giây, chỉ có 1 người được chạy đoạn code xét giá này
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                throw new Exception("Phiên đấu giá chưa mở hoặc đã kết thúc!");
            }

            if (newBid.getBidAmount() <= item.getCurrentHighestBid()) {
                throw new Exception("Giá đặt phải cao hơn mức giá cao nhất hiện tại!");
            }

            // Nếu hợp lệ: Cập nhật hệ thống
            bidHistory.add(newBid);
            winningBid = newBid;
            item.setCurrentHighestBid(newBid.getBidAmount());

            // THÔNG BÁO CHO TẤT CẢ GIAO DIỆN CẬP NHẬT REALTIME
            notifyObservers(newBid, "Có giá mới: $" + newBid.getBidAmount());

        } finally {
            // Luôn phải mở khóa trong khối finally để dù có lỗi xảy ra thì hệ thống không bị treo (Deadlock)
            lock.unlock();
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
    public AuctionStatus getStatus() { return status; }
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