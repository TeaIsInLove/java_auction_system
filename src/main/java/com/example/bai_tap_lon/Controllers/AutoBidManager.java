package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that tracks auto-bid registrations and fires bids automatically.
 *
 * <p>Data structure: auctionId → (username → maxPrice)</p>
 *
 * <p>When a bid is placed in AuctionWorkspace, {@link #triggerAutoBids} is called.
 * Any registered user whose max price exceeds the new current price will be automatically
 * outbid at (currentHighest + increment).</p>
 */
public final class AutoBidManager {

    /** Minimum step that an auto-bid adds above the current highest price (VNĐ). */
    private static final double AUTO_BID_INCREMENT = 500_000.0;

    private static final AutoBidManager INSTANCE = new AutoBidManager();

    /** auctionId → (username → maxPrice) */
    private final Map<String, Map<String, Double>> registrations = new ConcurrentHashMap<>();

    private AutoBidManager() {}

    public static AutoBidManager getInstance() {
        return INSTANCE;
    }

    /**
     * Register or update an auto-bid for the given user/auction pair.
     *
     * @param auctionId target auction
     * @param username  the bidder
     * @param maxPrice  maximum price the user is willing to pay
     */
    public void register(String auctionId, String username, double maxPrice) {
        registrations
                .computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                .put(username, maxPrice);
    }

    /** Remove the auto-bid registration for the given user/auction pair. */
    public void unregister(String auctionId, String username) {
        Map<String, Double> inner = registrations.get(auctionId);
        if (inner != null) {
            inner.remove(username);
        }
    }

    /** @return true if the user has an active auto-bid registration for this auction. */
    public boolean isRegistered(String auctionId, String username) {
        Map<String, Double> inner = registrations.get(auctionId);
        return inner != null && inner.containsKey(username);
    }

    /** @return the max price the user registered, or -1 if not registered. */
    public double getMaxPrice(String auctionId, String username) {
        Map<String, Double> inner = registrations.get(auctionId);
        if (inner == null) return -1;
        return inner.getOrDefault(username, -1.0);
    }

    /**
     * Called after every successful manual bid.  Checks all registered auto-bidders
     * (excluding the user who just bid) and fires one bid for the first eligible one.
     *
     * @param auction        the auction that just received a bid
     * @param lastBidder     username of the person who placed the triggering bid
     * @param workspace      the AuctionWorkspace singleton (passed in to avoid circular deps)
     */
    public void triggerAutoBids(Auction auction, String lastBidder, AuctionWorkspace workspace) {
        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
            return;
        }
        Map<String, Double> inner = registrations.get(auction.getId());
        if (inner == null || inner.isEmpty()) {
            return;
        }

        double current = auction.getItem().getCurrentHighestBid();

        for (Map.Entry<String, Double> entry : inner.entrySet()) {
            String autoUser = entry.getKey();
            double maxPrice = entry.getValue();

            // Skip the person who just bid (avoid infinite ping-pong)
            if (autoUser.equals(lastBidder)) {
                continue;
            }

            double nextBid = current + AUTO_BID_INCREMENT;
            if (nextBid <= maxPrice) {
                boolean ok = workspace.placeBid(auction, autoUser, nextBid);
                if (ok) {
                    // Only one auto-bidder fires per triggering bid to keep ordering fair
                    break;
                }
            }
        }
    }
}
