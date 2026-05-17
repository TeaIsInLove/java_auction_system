package com.example.bai_tap_lon.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final DatabaseManager databaseManager;

    public UserRepository() {
        this.databaseManager = new DatabaseManager();
    }

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the kiem tra email.", ex);
        }
    }

    public void save(AppUser user) {
        String sql = "INSERT INTO users(full_name, email, password_hash, role, balance) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.setDouble(5, user.getBalance());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the luu user.", ex);
        }
    }

    public Optional<AppUser> findByEmail(String email) {
        String sql = "SELECT full_name, email, password_hash, role, balance FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AppUser(
                        resultSet.getString("full_name"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("email"),
                        resultSet.getString("role"),
                        resultSet.getDouble("balance")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc user.", ex);
        }
    }

    public List<AppUser> findAll() {
        String sql = "SELECT full_name, email, password_hash, role, balance FROM users ORDER BY created_at DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AppUser> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(new AppUser(
                        resultSet.getString("full_name"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("email"),
                        resultSet.getString("role"),
                        resultSet.getDouble("balance")
                ));
            }
            return users;
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc danh sach user.", ex);
        }
    }

    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT full_name, email, password_hash, role, balance FROM users WHERE full_name = ? LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AppUser(
                        resultSet.getString("full_name"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("email"),
                        resultSet.getString("role"),
                        resultSet.getDouble("balance")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc user theo ten.", ex);
        }
    }

    public void updateBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE full_name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, newBalance);
            statement.setString(2, username);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Khong tim thay user: " + username);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the cap nhat so du.", ex);
        }
    }

    public void updateBalanceByEmail(String email, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE email = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, newBalance);
            statement.setString(2, email);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Khong tim thay user voi email: " + email);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the cap nhat so du theo email.", ex);
        }
    }
}
