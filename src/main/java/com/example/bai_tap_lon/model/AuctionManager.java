package com.example.bai_tap_lon.model;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    // 1. Biến static lưu trữ instance duy nhất
    private static AuctionManager instance;

    // Danh sách các phiên đấu giá trên toàn hệ thống
    private List<Auction> activeAuctions;

    // 2. Constructor private để ngăn chặn việc dùng từ khóa 'new' từ bên ngoài
    private AuctionManager() {
        activeAuctions = new ArrayList<>();
    }

    // 3. Phương thức public static để lấy instance duy nhất (có synchronized để an toàn trong đa luồng)
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        activeAuctions.add(auction);
    }

    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }
}