package com.example.bai_tap_lon.Services;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionManager;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;

import java.time.LocalDateTime;

public class DashboardService {

    /**
     * Phiên còn trong khung thời gian (chưa hết endTime) mà người dùng đã từng đặt giá.
     */
    public int getJoinedAuctionCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String key = username.trim();
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (Auction auction : AuctionManager.getInstance().getActiveAuctions()) {
            if (auction.getItem() == null || auction.getItem().getEndTime() == null) {
                continue;
            }
            if (!auction.getItem().getEndTime().isAfter(now)) {
                continue;
            }
            boolean hasBidFromUser = auction.getBidHistory().stream()
                    .map(BidTransaction::getBidder)
                    .anyMatch(bidder -> bidder != null
                            && key.equalsIgnoreCase(bidder.getUsername().trim()));
            if (hasBidFromUser) {
                count++;
            }
        }
        return count;
    }

    /**
     * Phiên đã kết thúc thành công (FINISHED) và người thắng trùng username đăng nhập.
     */
    public int getWonAuctionCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String key = username.trim();
        int count = 0;
        for (Auction auction : AuctionManager.getInstance().getActiveAuctions()) {
            if (auction.getStatus() != AuctionStatus.FINISHED) {
                continue;
            }
            if (auction.getWinningBid() == null || auction.getWinningBid().getBidder() == null) {
                continue;
            }
            String winner = auction.getWinningBid().getBidder().getUsername();
            if (key.equalsIgnoreCase(winner.trim())) {
                count++;
            }
        }
        return count;
    }

    public int getCreatedAuctionCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String key = username.trim();
        int count = 0;
        for (Auction auction : AuctionManager.getInstance().getActiveAuctions()) {
            if (auction.getSeller() != null && key.equalsIgnoreCase(auction.getSeller().getUsername().trim())) {
                count++;
            }
        }
        return count;
    }
}