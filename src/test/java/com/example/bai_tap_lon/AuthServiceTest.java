package com.example.bai_tap_lon;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.AuthService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private final AuthService authService = new AuthService();

    @Test
    void registerWithShortPasswordThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Test User", "test@example.com", "123"));
    }

    @Test
    void registerWithInvalidEmailThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Test User", "notanemail", "password123"));
    }

    @Test
    void registerWithEmptyNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "a@b.com", "password123"));
    }

    @Test
    void loginWithNullReturnsEmpty() {
        Optional<AppUser> result = authService.login(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void loginWithWrongCredentialsReturnsEmpty() {
        Optional<AppUser> result = authService.login("nonexistent@nowhere.com", "wrongpassword");
        assertTrue(result.isEmpty());
    }
}
