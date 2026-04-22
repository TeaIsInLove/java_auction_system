package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

// Lớp Item được khai báo abstract và kế thừa trực tiếp từ Entity
public abstract class Item extends Entity {

    // Các thuộc tính cơ bản của một sản phẩm
    private String name;
    private String description;
    private double startingPrice;

    // Các thuộc tính phục vụ cho phiên đấu giá
    private double currentHighestBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Constructor khởi tạo sản phẩm
    public Item(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(); // Gọi đến constructor của Entity để hệ thống tự động sinh ID và createdAt
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        // Khi mới tạo, giá cao nhất hiện tại chính là giá khởi điểm
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Áp dụng Đóng gói (Encapsulation): Sử dụng getter/setter cho các thuộc tính private

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    // Áp dụng Đa hình (Polymorphism): Ghi đè phương thức từ lớp cha Entity
    @Override
    public void printInfo() {
        System.out.println("--- Thông Tin Sản Phẩm ---");
        System.out.println("ID Sản phẩm: " + this.getId());
        System.out.println("Tên: " + this.name);
        System.out.println("Mô tả: " + this.description);
        System.out.println("Giá khởi điểm: " + this.startingPrice);
        System.out.println("Giá hiện tại cao nhất: " + this.currentHighestBid);
        System.out.println("Thời gian bắt đầu: " + this.startTime);
        System.out.println("Thời gian kết thúc: " + this.endTime);
    }

    // Phương thức trừu tượng bổ sung: Buộc các lớp con (Electronics, Art, Vehicle) phải tự định nghĩa loại của chúng
    public abstract String getItemCategory();
}