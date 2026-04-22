package com.example.bai_tap_lon.model;

public class Admin extends User {
    private int clearanceLevel; // Cấp độ quyền hạn (ví dụ: 1 là cao nhất)

    public Admin(String username, String password, String email, int clearanceLevel) {
        super(username, password, email);
        this.clearanceLevel = clearanceLevel;
    }

    public int getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(int clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Cấp độ quyền hạn: Mức " + this.clearanceLevel);
        System.out.println();
    }
}