package com.example.bai_tap_lon;

import com.example.bai_tap_lon.dao.UserDAO;
import com.example.bai_tap_lon.model.Bidder;
import com.example.bai_tap_lon.model.User;

public class Main {
    public static void main(String[] args) {
        // 1. Tạo một tài khoản mới tinh bằng Java
        System.out.println("Đang tạo tài khoản Bidder...");
        User newBidder = new Bidder("NguoiChoiHeX", "matkhauSieuKho123", "he_x@gmail.com");

        // 2. Gọi xe tải DAO đến chở dữ liệu đi cất
        System.out.println("Đang kết nối Database và lưu dữ liệu...");
        UserDAO userDao = new UserDAO();
        boolean isSuccess = userDao.insertUser(newBidder, "Bidder");

        // 3. Thông báo kết quả
        if (isSuccess) {
            System.out.println("✅ LƯU DỮ LIỆU THÀNH CÔNG RỰC RỠ!");
            System.out.println("👉 Hãy mở phpMyAdmin, bấm vào bảng 'users' để xem thành quả nhé!");
        } else {
            System.out.println("❌ LƯU THẤT BẠI. Vui lòng kiểm tra lại log lỗi.");
        }
    }
}