package com.example.bai_tap_lon.auth;

import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.model.auction.AuctionStatus;
import com.example.bai_tap_lon.model.entity.BidTransaction;
import com.example.bai_tap_lon.model.entity.item.*;
import com.example.bai_tap_lon.model.entity.user.Seller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class    AuctionRepository {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DatabaseManager databaseManager;
    private final BidRepository bidRepository;

    public AuctionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.bidRepository = new BidRepository(databaseManager);
    }

    public Auction findById(String id) {
        String sql = """
                SELECT id, item_type, item_name, description, starting_price, current_highest_bid,
                       start_time, end_time, seller_name, extra_info, status, created_at
                FROM auction_sessions WHERE id = ? LIMIT 1
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc phien dau gia.", ex);
        }
    }

    public List<Auction> findAll() {
        String sql = """
                SELECT id, item_type, item_name, description, starting_price, current_highest_bid,
                       start_time, end_time, seller_name, extra_info, status, created_at
                FROM auction_sessions
                ORDER BY start_time
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Auction> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc danh sach phien dau gia.", ex);
        }
    }

    public void insert(Auction auction) {
        Item item = auction.getItem();
        String sql = """
                INSERT INTO auction_sessions (
                    id, item_type, item_name, description, starting_price, current_highest_bid,
                    start_time, end_time, seller_name, extra_info, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAuction(statement, auction, item, 1);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the luu phien dau gia.", ex);
        }
    }

    public void delete(String auctionId) {
        String sql = "DELETE FROM auction_sessions WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the xoa phien dau gia.", ex);
        }
    }

    public void update(Auction auction) {
        String sql = """
                UPDATE auction_sessions
                SET status = ?, current_highest_bid = ?,
                    item_name = ?, description = ?, starting_price = ?,
                    start_time = ?, end_time = ?, seller_name = ?, extra_info = ?, item_type = ?
                WHERE id = ?
                """;
        Item item = auction.getItem();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, auction.getStatus().name());
            statement.setDouble(i++, item.getCurrentHighestBid());
            statement.setString(i++, item.getName());
            statement.setString(i++, nullToEmpty(item.getDescription()));
            statement.setDouble(i++, item.getStartingPrice());
            statement.setString(i++, item.getStartTime().format(ISO));
            statement.setString(i++, item.getEndTime().format(ISO));
            statement.setString(i++, auction.getSeller().getUsername());
            statement.setString(i++, resolveExtraInfo(item));
            statement.setString(i++, resolveItemType(item));
            statement.setString(i, auction.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the cap nhat phien dau gia.", ex);
        }
    }

    private void bindAuction(PreparedStatement statement, Auction auction, Item item, int startIndex)
            throws SQLException {
        int i = startIndex;
        statement.setString(i++, auction.getId());
        statement.setString(i++, resolveItemType(item));
        statement.setString(i++, item.getName());
        statement.setString(i++, nullToEmpty(item.getDescription()));
        statement.setDouble(i++, item.getStartingPrice());
        statement.setDouble(i++, item.getCurrentHighestBid());
        statement.setString(i++, item.getStartTime().format(ISO));
        statement.setString(i++, item.getEndTime().format(ISO));
        statement.setString(i++, auction.getSeller().getUsername());
        statement.setString(i++, resolveExtraInfo(item));
        statement.setString(i++, auction.getStatus().name());
        statement.setString(i, auction.getCreatedAt().format(ISO));
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String type = rs.getString("item_type");
        String itemName = rs.getString("item_name");
        String description = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        double currentHighest = rs.getDouble("current_highest_bid");
        LocalDateTime startTime = parseDateTime(rs.getString("start_time"));
        LocalDateTime endTime = parseDateTime(rs.getString("end_time"));
        String sellerName = rs.getString("seller_name");
        String extraInfo = rs.getString("extra_info");
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        LocalDateTime createdAt = parseDateTimeFlexible(rs.getString("created_at"));

        Item item = ItemFactory.createItem(type, itemName, description, startingPrice, startTime, endTime, extraInfo);
        item.setCurrentHighestBid(currentHighest);
        Seller seller = new Seller(sellerName, "", emailFromName(sellerName), sellerName);
        Auction auction = new Auction(item, seller);
        auction.setId(id);
        auction.setCreatedAt(createdAt);
        auction.restorePersistedState(status);

        // Load bid history from database
        List<BidTransaction> bidHistory = bidRepository.findByAuctionId(id);
        for (BidTransaction bid : bidHistory) {
            auction.addBidFromDatabase(bid);
        }

        return auction;
    }

    private static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value.trim(), ISO);
    }

    private static LocalDateTime parseDateTimeFlexible(String value) {
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
            throw e;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String resolveItemType(Item item) {
        if (item instanceof Electronics) {
            return "electronics";
        }
        if (item instanceof Art) {
            return "art";
        }
        if (item instanceof Vehicle) {
            return "vehicle";
        }
        throw new IllegalArgumentException("Loai san pham khong ho tro luu CSDL.");
    }

    private static String resolveExtraInfo(Item item) {
        if (item instanceof Electronics e) {
            return e.getBrand();
        }
        if (item instanceof Art a) {
            return a.getArtist();
        }
        if (item instanceof Vehicle v) {
            return v.getMake();
        }
        return "";
    }

    private static String emailFromName(String name) {
        String safeName = name == null ? "user" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".");
        if (safeName.isBlank()) {
            safeName = "user";
        }
        return safeName + "@local";
    }
}
