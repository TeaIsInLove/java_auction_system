package com.example.bai_tap_lon.model;

public class Seller extends User {
    private String shopName;
    private double reputationScore; // Điểm đánh giá độ uy tín (ví dụ: từ 1.0 đến 5.0)

    public Seller(String username, String password, String email, String shopName) {
        super(username, password, email);
        this.shopName = shopName;
        this.reputationScore = 5.0; // Mặc định khi mới tạo tài khoản là 5 sao
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public double getReputationScore() { return reputationScore; }
    public void setReputationScore(double reputationScore) { this.reputationScore = reputationScore; }

    @Override
    public String getRole() {
        return "Seller";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Tên cửa hàng: " + this.shopName);
        System.out.println("Điểm uy tín: " + this.reputationScore + " / 5.0");
        System.out.println();
    }
}