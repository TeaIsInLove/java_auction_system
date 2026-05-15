package com.example.bai_tap_lon.auth;

import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.user.Bidder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;

    public BidRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveBid(String auctionId, BidTransaction bid) {
        String sql = """
                INSERT INTO bid_history (id, auction_id, bidder_name, bidder_email, bid_amount, bid_time)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bid.getId());
            statement.setString(2, auctionId);
            statement.setString(3, bid.getBidder().getUsername());
            statement.setString(4, bid.getBidder().getEmail());
            statement.setDouble(5, bid.getBidAmount());
            statement.setString(6, bid.getBidTime().format(ISO));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the luu lich su dat gia.", ex);
        }
    }

    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = """
                SELECT id, bidder_name, bidder_email, bid_amount, bid_time
                FROM bid_history
                WHERE auction_id = ?
                ORDER BY bid_time ASC
                """;
        List<BidTransaction> bids = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc lich su dat gia.", ex);
        }
        return bids;
    }

    public int countByAuctionId(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_history WHERE auction_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the dem so lan dat gia.", ex);
        }
        return 0;
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String bidderName = rs.getString("bidder_name");
        String bidderEmail = rs.getString("bidder_email");
        double bidAmount = rs.getDouble("bid_amount");
        LocalDateTime bidTime = parseDateTime(rs.getString("bid_time"));

        Bidder bidder = new Bidder(bidderName, "", bidderEmail);
        BidTransaction bid = new BidTransaction(bidder, bidAmount);
        bid.setId(id);
        bid.setBidTime(bidTime);
        return bid;
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
