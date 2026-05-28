package com.example.bai_tap_lon.model.entity.item;

import java.time.LocalDateTime;

public class GeneralItem extends Item {
    private final String category;

    public GeneralItem(String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, String category) {
        super(name, description, startingPrice, startTime, endTime);
        this.category = (category != null && !category.isBlank()) ? category.trim() : "Khác";
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getItemCategory() {
        return category;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Danh mục: " + category);
        System.out.println("--------------------------");
    }
}
