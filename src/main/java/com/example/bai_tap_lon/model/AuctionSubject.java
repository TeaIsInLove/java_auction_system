package com.example.bai_tap_lon.model;

public interface AuctionSubject {
    void addObserver(AuctionObserver observer); // Thêm người theo dõi
    void removeObserver(AuctionObserver observer); // Xóa người theo dõi
    void notifyObservers(BidTransaction newBid, String message); // Phát thông báo
}