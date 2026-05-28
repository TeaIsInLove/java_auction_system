package com.example.bai_tap_lon.model.entity.item;

import java.time.LocalDateTime;

public class ItemFactory {
    // Phương thức Factory (Nhà máy) chuyên sản xuất Item
    public static Item createItem(String type, String name, String description, double startPrice,
                                  LocalDateTime start, LocalDateTime end, String extraInfo) {
        switch (type.toLowerCase()) {
            case "electronics":
                return new Electronics(name, description, startPrice, start, end, extraInfo, 12);
            case "art":
                return new Art(name, description, startPrice, start, end, extraInfo, 2024);
            case "vehicle":
                return new Vehicle(name, description, startPrice, start, end, extraInfo, "Unknown", 0.0);
            case "general":
                return new GeneralItem(name, description, startPrice, start, end, extraInfo);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}