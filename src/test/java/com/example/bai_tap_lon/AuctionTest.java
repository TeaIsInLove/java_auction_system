package com.example.bai_tap_lon;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.item.Electronics;
import com.example.bai_tap_lon.model.entity.user.Bidder;
import com.example.bai_tap_lon.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        Electronics item = new Electronics(
                "Laptop", "Gaming laptop", 10_000_000,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                "Dell", 12
        );
        Seller seller = new Seller("seller1", "pass", "seller1@test.com", "ShopA");
        auction = new Auction(item, seller);
        bidder = new Bidder("buyer1", "pass", "buyer1@test.com", 50_000_000);
    }

    @Test
    void newAuctionIsOpen() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void startAuctionTransitionsToRunning() {
        auction.startAuction();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void startAuctionIsIdempotentWhenAlreadyRunning() {
        auction.startAuction();
        auction.startAuction(); // second call should be a no-op
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void placeBidFailsWhenNotRunning() {
        BidTransaction bid = new BidTransaction(bidder, 15_000_000);
        assertThrows(Exception.class, () -> auction.placeBid(bid));
    }

    @Test
    void placeBidSucceedsWhenRunning() throws Exception {
        auction.startAuction();
        BidTransaction bid = new BidTransaction(bidder, 15_000_000);
        auction.placeBid(bid);

        assertEquals(15_000_000, auction.getItem().getCurrentHighestBid());
        assertEquals(bid, auction.getWinningBid());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void placeBidFailsWhenAmountTooLow() {
        auction.startAuction();
        BidTransaction lowBid = new BidTransaction(bidder, 5_000_000); // below starting price
        assertThrows(Exception.class, () -> auction.placeBid(lowBid));
    }

    @Test
    void higherBidWins() throws Exception {
        auction.startAuction();
        Bidder bidder2 = new Bidder("buyer2", "pass", "buyer2@test.com", 100_000_000);

        auction.placeBid(new BidTransaction(bidder, 12_000_000));
        auction.placeBid(new BidTransaction(bidder2, 15_000_000));

        assertEquals(bidder2.getUsername(), auction.getWinningBid().getBidder().getUsername());
        assertEquals(15_000_000, auction.getItem().getCurrentHighestBid());
        assertEquals(2, auction.getBidHistory().size());
    }

    @Test
    void endAuctionWithWinnerBecomesFinished() throws Exception {
        auction.startAuction();
        auction.placeBid(new BidTransaction(bidder, 12_000_000));
        auction.endAuction();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void endAuctionWithoutBidsBecomesCanceled() {
        auction.startAuction();
        auction.endAuction();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void cancelAuctionWorksWhenNoBids() {
        auction.cancelAuction();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void cancelAuctionDoesNotWorkAfterBidPlaced() throws Exception {
        auction.startAuction();
        auction.placeBid(new BidTransaction(bidder, 12_000_000));
        auction.endAuction(); // Finish it first
        // cancelAuction only works on empty bid history
        // bidHistory is not empty, so status should NOT change to CANCELED via cancelAuction
        // (auction is already FINISHED at this point; cancelAuction checks isEmpty)
        AuctionStatus before = auction.getStatus();
        auction.cancelAuction();
        assertEquals(before, auction.getStatus()); // status unchanged
    }

    @Test
    void markAsPaidTransitionsToPaid() throws Exception {
        auction.startAuction();
        auction.placeBid(new BidTransaction(bidder, 12_000_000));
        auction.endAuction();
        auction.markAsPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }
}
