package com.example.bai_tap_lon.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. Áp dụng Singleton: Lưu duy nhất 1 instance kết nối
    private static Connection connection;

    // Thay đổi thông tin này cho khớp với máy của bạn
    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Pass máy bạn (XAMPP thường để trống)

    // 2. Private constructor
    private DatabaseConnection() {}

    // 3. Hàm lấy kết nối
    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                // Tải Driver và mở kết nối
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(">> Kết nối Database MySQL thành công!");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println(">> Lỗi kết nối Database: " + e.getMessage());
            }
        }
        return connection;
    }
}
