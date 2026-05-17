package com.example.bai_tap_lon.model.entity.user;

public class Seller extends User {
    private String shopName;
    private double reputationScore; // Điểm đánh giá độ uy tín (ví dụ: từ 1.0 đến 5.0)
    private double balance; // Số dư tài khoản người bán (nhận tiền từ đấu giá)

    public Seller(String username, String password, String email, String shopName) {
        super(username, password, email);
        this.shopName = shopName;
        this.reputationScore = 5.0; // Mặc định khi mới tạo tài khoản là 5 sao
        this.balance = 0.0;
    }

    public Seller(String username, String password, String email, String shopName, double initialBalance) {
        super(username, password, email);
        this.shopName = shopName;
        this.reputationScore = 5.0;
        this.balance = initialBalance;
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public double getReputationScore() { return reputationScore; }
    public void setReputationScore(double reputationScore) { this.reputationScore = reputationScore; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // Cộng tiền vào tài khoản (khi bán được hàng)
    public void credit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    // Trừ tiền từ tài khoản
    public boolean debit(double amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String getRole() {
        return "Seller";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Tên cửa hàng: " + this.shopName);
        System.out.println("Điểm uy tín: " + this.reputationScore + " / 5.0");
        System.out.println("Số dư tài khoản: $" + String.format("%.2f", this.balance));
        System.out.println();
    }
}