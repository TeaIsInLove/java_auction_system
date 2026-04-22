package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private Bidder bidder;       // Người đặt giá
    private double bidAmount;    // Số tiền đặt
    private LocalDateTime bidTime; // Thời điểm đặt giá

    public BidTransaction(Bidder bidder, double bidAmount) {
        super(); // Tự động sinh ID giao dịch
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now(); // Lấy thời gian thực lúc gọi lệnh
    }

    // Getters
    public Bidder getBidder() { return bidder; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }

    @Override
    public void printInfo() {
        System.out.println("Giao dịch ID: " + this.getId());
        System.out.println("Người đặt: " + bidder.getUsername());
        System.out.println("Mức giá: $" + bidAmount);
        System.out.println("Thời gian: " + bidTime);
    }
}