package com.example.bai_tap_lon.model.auction;

import com.example.bai_tap_lon.model.entity.BidTransaction;

public interface AuctionObserver {
    // Hàm này sẽ được gọi tự động khi có biến động về giá
    void update(Auction auction, BidTransaction newBid, String message);
}