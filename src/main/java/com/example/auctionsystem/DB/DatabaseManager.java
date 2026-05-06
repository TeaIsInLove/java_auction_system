package com.example.auctionsystem.DB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
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
                            created_at TEXT DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                try {
                    statement.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'");
                } catch (SQLException ignored) {
                    // column already exists
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Khong the khoi tao co so du lieu SQLite.", ex);
        }
    }
}
