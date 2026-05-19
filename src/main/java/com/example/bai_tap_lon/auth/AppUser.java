package com.example.bai_tap_lon.auth;

public class AppUser {
    private final String username;
    private final String password;
    private final String email;
    private final String role;
    private final double balance;

    public AppUser(String username, String password, String email, String role) {
        this(username, password, email, role, 0.0);
    }

    public AppUser(String username, String password, String email, String role, double balance) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public double getBalance() {
        return balance;
    }
}
