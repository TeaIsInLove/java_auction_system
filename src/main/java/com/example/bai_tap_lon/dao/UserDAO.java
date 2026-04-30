package com.example.bai_tap_lon.dao;

import com.example.bai_tap_lon.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {

    // Hàm nhận vào 1 Object User từ Logic và lưu bằng mã SQL
    public boolean insertUser(User user, String role) {
        // Mã SQL thuần túy
        String sql = "INSERT INTO users (id, username, password, email, role, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            // Lấy đường ống kết nối
            Connection conn = DatabaseConnection.getConnection();

            // Chuẩn bị câu lệnh (PreparedStatement giúp chống hack SQL Injection)
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, role); // "Bidder", "Seller" hoặc "Admin"
            pstmt.setString(6, user.getCreatedAt().toString());

            // Thực thi lệnh INSERT
            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0; // Trả về true nếu thêm thành công

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}