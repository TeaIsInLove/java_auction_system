package com.example.bai_tap_lon.model;

import java.util.UUID;
import java.time.LocalDateTime;

public abstract class Entity {
    // Sử dụng protected để các lớp con (Item, User) có thể kế thừa và truy cập trực tiếp nếu cần
    protected String id;
    protected LocalDateTime createdAt;

    // Constructor mặc định: Tự động tạo ID ngẫu nhiên (UUID) và lưu thời gian tạo
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Constructor có tham số (dùng khi đọc dữ liệu từ File/Database hoặc qua Mạng)
    public Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    // Đảm bảo tính đóng gói (Encapsulation) bằng Getter và Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Phương thức trừu tượng (Abstraction/Polymorphism):
    // Bắt buộc các lớp con (Bidder, Seller, Electronics, v.v.) phải tự định nghĩa cách hiển thị thông tin
    public abstract void printInfo();
}
