package com.example.bai_tap_lon.model.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// Model giao dịch thanh toán khi kết thúc đấu giá
public class PaymentTransaction {
    private final String id;
    private final String auctionId;
    private final String auctionName;
    private final String buyerUsername;
    private final String buyerEmail;
    private final String sellerUsername;
    private final String sellerEmail;
    private final double amount;
    private final LocalDateTime transactionTime;
    private final TransactionStatus status;

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }

    public PaymentTransaction(String auctionId, String auctionName,
                              String buyerUsername, String buyerEmail,
                              String sellerUsername, String sellerEmail,
                              double amount) {
        this.id = UUID.randomUUID().toString();
        this.auctionId = auctionId;
        this.auctionName = auctionName;
        this.buyerUsername = buyerUsername;
        this.buyerEmail = buyerEmail;
        this.sellerUsername = sellerUsername;
        this.sellerEmail = sellerEmail;
        this.amount = amount;
        this.transactionTime = LocalDateTime.now();
        this.status = TransactionStatus.COMPLETED;
    }

    public String getId() { return id; }
    public String getAuctionId() { return auctionId; }
    public String getAuctionName() { return auctionName; }
    public String getBuyerUsername() { return buyerUsername; }
    public String getBuyerEmail() { return buyerEmail; }
    public String getSellerUsername() { return sellerUsername; }
    public String getSellerEmail() { return sellerEmail; }
    public double getAmount() { return amount; }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public TransactionStatus getStatus() { return status; }

    public String getFormattedAmount() {
        return String.format("$%,.2f", amount);
    }

    public String getFormattedTime() {
        return transactionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s (Auction: %s)",
                getFormattedTime(),
                buyerUsername,
                sellerUsername,
                getFormattedAmount(),
                auctionName);
    }

    // Format cho database storage
    public String toDatabaseString() {
        DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%.2f|%s|%s",
                id, auctionId, auctionName,
                buyerUsername, buyerEmail,
                sellerUsername, sellerEmail,
                amount, transactionTime.format(ISO), status.name());
    }

    // Parse từ database string
    public static PaymentTransaction fromDatabaseString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length < 11) {
            throw new IllegalArgumentException("Invalid payment transaction data");
        }

        PaymentTransaction pt = new PaymentTransaction(
                parts[1], parts[2],
                parts[3], parts[4],
                parts[5], parts[6],
                Double.parseDouble(parts[7])
        );
        return pt;
    }
}
