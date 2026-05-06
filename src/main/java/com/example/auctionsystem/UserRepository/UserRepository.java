package com.example.auctionsystem.UserRepository;

import com.example.auctionsystem.DB.DatabaseManager;
import com.example.auctionsystem.Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final DatabaseManager databaseManager;

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

    public void save(User user) {
        String sql = "INSERT INTO users(full_name, email, password_hash, role) VALUES(?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the luu user.", ex);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT full_name, email, password_hash, role FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new User(
                                resultSet.getString("full_name"),
                                resultSet.getString("email"),
                                resultSet.getString("password_hash"),
                                resultSet.getString("role")
                        )
                );
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc user.", ex);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT full_name, email, password_hash, role FROM users ORDER BY created_at DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getString("full_name"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("role")
                ));
            }
            return users;
        } catch (SQLException ex) {
            throw new RuntimeException("Khong the doc danh sach user.", ex);
        }
    }
}
