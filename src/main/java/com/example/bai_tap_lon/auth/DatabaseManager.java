package com.example.bai_tap_lon.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Path DB_PATH = Paths.get(
            System.getProperty("user.home"),
            ".auctionsystem",
            "app.db"
    );
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public DatabaseManager() {
        initializeDatabase();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    private void initializeDatabase() {
        try {
            Files.createDirectories(DB_PATH.getParent());
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            full_name TEXT NOT NULL,
                            email TEXT NOT NULL UNIQUE,
                            password_hash TEXT NOT NULL,
                            role TEXT NOT NULL DEFAULT 'USER',
                            balance REAL NOT NULL DEFAULT 100000000,
                            created_at TEXT DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS auction_sessions (
                            id TEXT PRIMARY KEY,
                            item_type TEXT NOT NULL,
                            item_name TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            starting_price REAL NOT NULL,
                            current_highest_bid REAL NOT NULL,
                            start_time TEXT NOT NULL,
                            end_time TEXT NOT NULL,
                            seller_name TEXT NOT NULL,
                            extra_info TEXT NOT NULL DEFAULT '',
                            status TEXT NOT NULL DEFAULT 'OPEN',
                            created_at TEXT NOT NULL DEFAULT (datetime('now'))
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS bid_history (
                            id TEXT PRIMARY KEY,
                            auction_id TEXT NOT NULL,
                            bidder_name TEXT NOT NULL,
                            bidder_email TEXT NOT NULL,
                            bid_amount REAL NOT NULL,
                            bid_time TEXT NOT NULL,
                            FOREIGN KEY (auction_id) REFERENCES auction_sessions(id)
                        )
                        """);

                statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_auction_id
                        ON bid_history(auction_id)
                        """);

                // Migration: Thêm column balance nếu chưa có (cho database cũ)
                try {
                    statement.execute("ALTER TABLE users ADD COLUMN balance REAL NOT NULL DEFAULT 100000000");
                } catch (SQLException e) {
                    // Column đã tồn tại, bỏ qua
                }

                // Seed tài khoản admin mặc định nếu chưa tồn tại
                seedDefaultAdmin(statement);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Khong the khoi tao co so du lieu SQLite.", ex);
        }
    }

    private static void seedDefaultAdmin(Statement statement) throws SQLException {
        String adminEmail = "admin@local";
        String adminName = "Administrator";
        String adminPasswordHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        String adminRole = "ADMIN";

        ResultSet rs = statement.executeQuery(
                "SELECT 1 FROM users WHERE email = '" + adminEmail + "' LIMIT 1");
        boolean adminExists = rs.next();
        rs.close();

        if (!adminExists) {
            try (PreparedStatement ps = statement.getConnection().prepareStatement(
                    "INSERT INTO users(full_name, email, password_hash, role, balance) VALUES(?, ?, ?, ?, ?)")) {
                ps.setString(1, adminName);
                ps.setString(2, adminEmail);
                ps.setString(3, adminPasswordHash);
                ps.setString(4, adminRole);
                ps.setDouble(5, 100_000_000.0);
                ps.executeUpdate();
            }
        }
    }
}
