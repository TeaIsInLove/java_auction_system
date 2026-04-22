package com.example.bai_tap_lon.model;

public abstract class User extends Entity {
    // các thuộc tính cơ bản của một người dùng hệ thống
    private String username;
    private String password;
    private String email;
    // cóntructor khỏi tạo người dùng
    public User(String username, String password, String email){
        this.username=username;
        this.password=password;
        this.email=email;
    }
    // Áp dụng Đóng gói (Encapsulation): Getter và Setter cho các thuộc tính private
    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username=username;
    }

    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password=password;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email=email;
    }
    // Phương thức trừu tượng: Buộc các lớp con (Bidder, Seller, Admin) phải xác định vai trò của mình
    public abstract String getRole();
    @Override
    public void printInfo() {
        System.out.println("--- Thông Tin Người Dùng ---");
        System.out.println("ID User: " + this.getId());
        System.out.println("Tên đăng nhập: " + this.username);
        System.out.println("Email: " + this.email);
        System.out.println("Vai trò: " + this.getRole());
        System.out.println("Ngày tham gia: " + this.getCreatedAt());
        System.out.println("----------------------------");
    }
}

