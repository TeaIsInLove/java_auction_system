package com.example.bai_tap_lon.model;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;                  // Sản phẩm được đấu giá
    private Seller seller;              // Người tổ chức (người bán)
    private AuctionStatus status;       // Trạng thái hiện tại
    private List<BidTransaction> bidHistory; // Lịch sử các lần đặt giá
    private BidTransaction winningBid;  // Giao dịch thắng cuộc (lưu tạm hoặc chốt cuối)

    public Auction(Item item, Seller seller) {
        super(); // Tự động tạo ID cho phiên đấu giá
        this.item = item;
        this.seller = seller;
        this.status = AuctionStatus.OPEN; // Mặc định khi mới tạo là OPEN
        this.bidHistory = new ArrayList<>();
    }

    // Đổi trạng thái phiên đấu giá
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public AuctionStatus getStatus() {
        return this.status;
    }

    /**
     * Phương thức đặt giá.
     * Dùng 'synchronized' để chặn race condition khi nhiều client gọi cùng lúc.
     */
    public synchronized void placeBid(BidTransaction newBid) throws Exception {
        // 1. Kiểm tra ngoại lệ: Đấu giá khi phiên đã đóng
        if (this.status != AuctionStatus.RUNNING) {
            throw new Exception("Phiên đấu giá chưa mở hoặc đã kết thúc!");
            // Tuần 8 các bạn sẽ tự viết Custom Exception thay cho Exception chung này
        }

        // 2. Kiểm tra ngoại lệ: Đặt giá thấp hơn hoặc bằng giá hiện tại
        if (newBid.getBidAmount() <= item.getCurrentHighestBid()) {
            throw new Exception("Giá đặt phải cao hơn mức giá cao nhất hiện tại ($"
                    + item.getCurrentHighestBid() + ")");
        }

        // 3. Nếu hợp lệ: Cập nhật dữ liệu
        bidHistory.add(newBid);
        winningBid = newBid; // Tạm thời người này đang dẫn đầu
        item.setCurrentHighestBid(newBid.getBidAmount()); // Cập nhật giá trên Item

        System.out.println(">> Đặt giá thành công! Người dẫn đầu hiện tại: "
                + newBid.getBidder().getUsername() + " với $" + newBid.getBidAmount());
    }

    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===");
        System.out.println("ID Phiên: " + this.getId());
        System.out.println("Người bán: " + seller.getUsername());
        System.out.println("Trạng thái: " + this.status);
        System.out.println("Tổng số lượt ra giá: " + bidHistory.size());
        if (winningBid != null) {
            System.out.println("Người đang dẫn đầu/Chiến thắng: " + winningBid.getBidder().getUsername()
                    + " ($" + winningBid.getBidAmount() + ")");
        }
        item.printInfo();
        System.out.println("===============================");
    }
}