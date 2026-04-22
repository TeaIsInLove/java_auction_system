package com.example.bai_tap_lon.model;

public enum AuctionStatus {
    OPEN,       // Vừa tạo, chưa bắt đầu
    RUNNING,    // Đang diễn ra, cho phép đặt giá
    FINISHED,   // Đã kết thúc thời gian
    PAID,       // Người thắng đã thanh toán
    CANCELED    // Bị hủy (ví dụ: không ai đặt giá)
}
