package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class ItemFactory {
    // Phương thức Factory (Nhà máy) chuyên sản xuất Item
    public static Item createItem(String type, String name, String description, double startPrice,
                                  LocalDateTime start, LocalDateTime end, String extraInfo) {
        switch (type.toLowerCase()) {
            case "electronics":
                // extraInfo có thể là thương hiệu
                return new Electronics(name, description, startPrice, start, end, extraInfo, 12);
            case "art":
                // extraInfo có thể là tên tác giả
                return new Art(name, description, startPrice, start, end, extraInfo, 2024);
            case "vehicle":
                // extraInfo có thể là hãng xe
                return new Vehicle(name, description, startPrice, start, end, extraInfo, "Unknown", 0.0);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}