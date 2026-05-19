package com.example.bai_tap_lon.auth;

import com.example.bai_tap_lon.model.entity.PaymentTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Repository để lưu trữ và truy vấn các giao dịch thanh toán
public class PaymentRepository {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public PaymentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // Tạo bảng payment_transactions nếu chưa tồn tại
    public void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS payment_transactions (
                id TEXT PRIMARY KEY,
                auction_id TEXT NOT NULL,
                auction_name TEXT NOT NULL,
                buyer_username TEXT NOT NULL,
                buyer_email TEXT NOT NULL,
                seller_username TEXT NOT NULL,
                seller_email TEXT NOT NULL,
                amount REAL NOT NULL,
                transaction_time TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'COMPLETED'
            )
            """;

        try (Connection connection = databaseManager.getConnection();
             java.sql.Statement statement = connection.createStatement()) {

            statement.execute(sql);

        } catch (SQLException ex) {
            throw new RuntimeException("Khong the tao bang payment_transactions.", ex);
        }
    }

    public void save(PaymentTransaction transaction) {
        String sql = """
                INSERT INTO payment_transactions (
                    id, auction_id, auction_name,
                    buyer_username, buyer_email,
                    seller_username, seller_email,
                    amount, transaction_time, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.getId());
            statement.setString(2, transaction.getAuctionId());
            statement.setString(3, transaction.getAuctionName());
            statement.setString(4, transaction.getBuyerUsername());
            statement.setString(5, transaction.getBuyerEmail());
            statement.setString(6, transaction.getSellerUsername());
            statement.setString(7, transaction.getSellerEmail());
            statement.setDouble(8, transaction.getAmount());
            statement.setString(9, transaction.getTransactionTime().format(ISO));
            statement.setString(10, transaction.getStatus().name());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the luu giao dich thanh toan.", ex);
        }
    }

    public List<PaymentTransaction> findByAuctionId(String auctionId) {
        String sql = """
                SELECT id, auction_id, auction_name,
                       buyer_username, buyer_email,
                       seller_username, seller_email,
                       amount, transaction_time, status
                FROM payment_transactions
                WHERE auction_id = ?
                ORDER BY transaction_time DESC
                """;
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc lich su giao dich.", ex);
        }
        return transactions;
    }

    public List<PaymentTransaction> findByBuyer(String buyerUsername) {
        String sql = """
                SELECT * FROM payment_transactions
                WHERE buyer_username = ?
                ORDER BY transaction_time DESC
                """;
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, buyerUsername);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc lich su giao dich theo nguoi mua.", ex);
        }
        return transactions;
    }

    public List<PaymentTransaction> findBySeller(String sellerUsername) {
        String sql = """
                SELECT * FROM payment_transactions
                WHERE seller_username = ?
                ORDER BY transaction_time DESC
                """;
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sellerUsername);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc lich su giao dich theo nguoi ban.", ex);
        }
        return transactions;
    }

    public List<PaymentTransaction> findAll() {
        String sql = "SELECT * FROM payment_transactions ORDER BY transaction_time DESC";
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                transactions.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc tat ca giao dich.", ex);
        }
        return transactions;
    }

    private PaymentTransaction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String auctionId = rs.getString("auction_id");
        String auctionName = rs.getString("auction_name");
        String buyerUsername = rs.getString("buyer_username");
        String buyerEmail = rs.getString("buyer_email");
        String sellerUsername = rs.getString("seller_username");
        String sellerEmail = rs.getString("seller_email");
        double amount = rs.getDouble("amount");
        LocalDateTime transactionTime = parseDateTime(rs.getString("transaction_time"));
        PaymentTransaction.TransactionStatus status =
                PaymentTransaction.TransactionStatus.valueOf(rs.getString("status"));

        // Tạo transaction object (id đã được set sẵn)
        PaymentTransaction pt = new PaymentTransaction(
                auctionId, auctionName,
                buyerUsername, buyerEmail,
                sellerUsername, sellerEmail,
                amount
        );
        return pt;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String v = value.trim();
        try {
            return LocalDateTime.parse(v, ISO);
        } catch (DateTimeParseException e) {
            if (v.contains(" ") && !v.contains("T")) {
                return LocalDateTime.parse(v.replace(' ', 'T'), ISO);
            }
            return LocalDateTime.now();
        }
    }
}
