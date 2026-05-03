package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.pattern.AuctionObserver;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TUẦN 7 - Core model: Phiên đấu giá
 *
 * Concurrency:
 *   - ReentrantLock bảo vệ placeBid() → tránh race condition / lost update
 *   - CopyOnWriteArrayList cho bids → đọc nhiều, ghi ít, thread-safe
 *   - volatile AuctionStatus → đảm bảo visibility giữa các thread
 *
 * Observer: notify tất cả observers khi có bid mới hoặc đóng phiên
 *
 * State machine: OPEN → RUNNING → FINISHED → PAID/CANCELED
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── Fields ──────────────────────────────────────────────────────────────
    private final String auctionId;
    private final Item   item;
    private final String sellerId;
    private double startingPrice;
    private double currentPrice;
    private double minimumIncrement; // bước giá tối thiểu

    private volatile AuctionStatus status; // volatile: mọi thread thấy ngay khi đổi
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Thread-safe list of bids (đọc nhiều >> ghi ít)
    private final List<Bid> bids = new CopyOnWriteArrayList<>();

    private String winnerId; // userId của người thắng (null nếu chưa kết thúc)

    // ── Concurrency lock ────────────────────────────────────────────────────
    // ReentrantLock thay vì synchronized để có tryLock, fairness, v.v.
    private final transient ReentrantLock bidLock = new ReentrantLock(true); // fair=true

    // ── Observers (transient: không serialize danh sách socket) ────────────
    private final transient List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // ── Constructor ─────────────────────────────────────────────────────────
    public Auction(String auctionId, Item item, String sellerId,
                   double startingPrice, double minimumIncrement,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId        = auctionId;
        this.item             = item;
        this.sellerId         = sellerId;
        this.startingPrice    = startingPrice;
        this.currentPrice     = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.startTime        = startTime;
        this.endTime          = endTime;
        this.status           = AuctionStatus.OPEN;
    }

    // ── Observer management ─────────────────────────────────────────────────
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyBidPlaced(Bid bid) {
        for (AuctionObserver obs : observers) {
            try { obs.onBidPlaced(this, bid); }
            catch (Exception e) {
                System.err.println("[Auction] Observer error on bid: " + e.getMessage());
            }
        }
    }

    private void notifyAuctionClosed() {
        for (AuctionObserver obs : observers) {
            try { obs.onAuctionClosed(this); }
            catch (Exception e) {
                System.err.println("[Auction] Observer error on close: " + e.getMessage());
            }
        }
    }

    // ── State transitions ────────────────────────────────────────────────────
    /**
     * OPEN → RUNNING
     * Gọi khi thời gian bắt đầu đến (do ScheduledExecutorService trong AuctionManager)
     */
    public void start() {
        bidLock.lock();
        try {
            if (status != AuctionStatus.OPEN) return;
            status = AuctionStatus.RUNNING;
            System.out.println("[Auction] Started: " + auctionId);
        } finally {
            bidLock.unlock();
        }
    }

    /**
     * RUNNING → FINISHED | CANCELED
     * Gọi khi hết giờ (ScheduledExecutorService) hoặc admin hủy.
     */
    public void close() {
        bidLock.lock();
        try {
            if (status != AuctionStatus.RUNNING && status != AuctionStatus.OPEN) return;

            if (bids.isEmpty()) {
                status = AuctionStatus.CANCELED;
                System.out.println("[Auction] Canceled (no bids): " + auctionId);
            } else {
                status   = AuctionStatus.FINISHED;
                // Người thắng = người đặt bid cao nhất = bid cuối cùng
                winnerId = bids.get(bids.size() - 1).getBidderId();
                System.out.println("[Auction] Finished. Winner: " + winnerId
                                 + " at $" + currentPrice);
            }
        } finally {
            bidLock.unlock();
        }
        notifyAuctionClosed(); // gọi ngoài lock để tránh deadlock
    }

    /**
     * FINISHED → PAID
     */
    public void markPaid() {
        bidLock.lock();
        try {
            if (status == AuctionStatus.FINISHED) {
                status = AuctionStatus.PAID;
            }
        } finally {
            bidLock.unlock();
        }
    }

    // ── Core business logic: PLACE BID ──────────────────────────────────────
    /**
     * TUẦN 7 - CRITICAL SECTION: đặt giá
     *
     * Tại sao dùng ReentrantLock:
     *   Nếu 2 client gửi bid $500 cùng lúc mà không lock,
     *   cả hai đều thấy currentPrice=$400, đều pass kiểm tra,
     *   → cả hai đều được chấp nhận → lost update, dữ liệu sai.
     *
     * Với ReentrantLock(fair=true):
     *   Thread nào đến trước được vào trước (FIFO) → công bằng.
     *
     * @throws AuctionClosedException nếu phiên không RUNNING
     * @throws InvalidBidException    nếu giá không hợp lệ
     */
    public Bid placeBid(String bidderId, double amount)
            throws AuctionClosedException, InvalidBidException {

        bidLock.lock(); // ← chỉ 1 thread được vào đây tại 1 thời điểm
        try {
            // Kiểm tra 1: phiên phải đang RUNNING
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                    "Auction " + auctionId + " is not running (status=" + status + ")");
            }

            // Kiểm tra 2: giá phải cao hơn currentPrice + minimumIncrement
            double minRequired = currentPrice + minimumIncrement;
            if (amount < minRequired) {
                throw new InvalidBidException(
                    "Bid $" + amount + " too low. Minimum required: $" + minRequired);
            }

            // Kiểm tra 3: bidder không thể tự bid cho auction của chính mình
            if (bidderId.equals(sellerId)) {
                throw new InvalidBidException("Seller cannot bid on their own auction.");
            }

            // Tất cả OK → cập nhật giá và lưu bid
            currentPrice = amount;
            Bid bid = new Bid(UUID.randomUUID().toString(), auctionId, bidderId, amount);
            bids.add(bid);

            System.out.printf("[Auction] Bid placed: %s -> $%.2f on %s%n",
                              bidderId, amount, auctionId);
            return bid;

        } finally {
            bidLock.unlock(); // ← LUÔN unlock dù có exception
        }
        // Notify observers SAU KHI unlock để tránh deadlock
        // (dùng biến local bid để tránh null - thực tế cần refactor nhỏ)
    }

    /**
     * Wrapper placeBid: unlock trước rồi notify.
     * Tách khỏi placeBid() để đảm bảo notify KHÔNG nằm trong lock.
     */
    public Bid placeBidAndNotify(String bidderId, double amount)
            throws AuctionClosedException, InvalidBidException {
        Bid bid = placeBid(bidderId, amount);
        notifyBidPlaced(bid); // gọi ngoài lock
        return bid;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getAuctionId()        { return auctionId; }
    public Item   getItem()             { return item; }
    public String getSellerId()         { return sellerId; }
    public double getStartingPrice()    { return startingPrice; }
    public double getCurrentPrice()     { return currentPrice; }
    public double getMinimumIncrement() { return minimumIncrement; }
    public AuctionStatus getStatus()    { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }
    public String getWinnerId()         { return winnerId; }

    /** Trả về snapshot bất biến (tránh expose internal list) */
    public List<Bid> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public Bid getLatestBid() {
        return bids.isEmpty() ? null : bids.get(bids.size() - 1);
    }
}
