package com.example.bai_tap_lon.model.auction;

public enum AuctionStatus {
    PENDING_APPROVAL, // Vừa tạo, chờ admin duyệt (tự xóa sau 12 giờ nếu chưa duyệt)
    OPEN,             // Đã được duyệt, chưa bắt đầu
    RUNNING,          // Đang diễn ra, cho phép đặt giá
    FINISHED,         // Đã kết thúc thời gian
    PAID,             // Người thắng đã thanh toán
    CANCELED          // Bị hủy (ví dụ: không ai đặt giá)
}
