package com.example.bai_tap_lon.exception;

// Kế thừa Exception để tạo lỗi tùy chỉnh
public class AuctionClosedException extends Exception{
    public AuctionClosedException(String message) {
        super(message);
    }
}
