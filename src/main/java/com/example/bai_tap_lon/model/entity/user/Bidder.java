package com.example.bai_tap_lon.model.entity.user;

public class Bidder extends User {
    private double balance; // Số dư tài khoản để tham gia đấu giá

    public Bidder(String username, String password, String email){
        super(username, password, email);
        this.balance=0.0;
    }
    public Bidder(String username, String password, String email, double initialBalance){
        super(username, password, email);
        this.balance = initialBalance;
    }
    public double getBalance(){
        return  balance;
    }
    public void setBalance(double balance){
        this.balance=balance;
    }
    // Nạp thêm tiền vào ví
    public void deposit(double amount){
        if(amount > 0){
            this.balance += amount;
        }
    }

    // Rút tiền từ ví (thanh toán khi thắng đấu giá)
    public boolean withdraw(double amount){
        if(amount > 0 && this.balance >= amount){
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // Kiểm tra số dư có đủ để bid không
    public boolean hasEnoughBalance(double amount){
        return this.balance >= amount;
    }
    @Override
    public String getRole(){
        return "Bidder";
    }
    @Override
    public void printInfo(){
        super.printInfo(); // Gọi phương thức printInfo() của lớp User
        System.out.println("Số dư tài khoản: $" + this.balance);
        System.out.println();
    }
}
