package com.example.bai_tap_lon;

import com.example.bai_tap_lon.auth.AuthService;
import com.example.bai_tap_lon.model.entity.user.Bidder;

/** Console smoke-test entry point (not the JavaFX app — use Launcher for that). */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Auction System - Console Test ===");

        Bidder bidder = new Bidder("TestUser", "pass", "test@example.com", 100_000_000);
        System.out.println("Created bidder: " + bidder.getUsername() + " | role: " + bidder.getRole());

        AuthService authService = new AuthService();
        try {
            boolean registered = authService.register("TestUser", "test_smoke@example.com", "password123");
            System.out.println("Registration: " + (registered ? "SUCCESS" : "Email already exists"));
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }

        System.out.println("=== Done ===");
    }
}
