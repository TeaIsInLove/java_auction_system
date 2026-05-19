package com.example.bai_tap_lon;

import com.example.bai_tap_lon.auth.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashIsNotNullOrEmpty() {
        String hash = PasswordUtil.hashPassword("secret");
        assertNotNull(hash);
        assertFalse(hash.isBlank());
    }

    @Test
    void samePasswordProducesSameHash() {
        assertEquals(PasswordUtil.hashPassword("password123"), PasswordUtil.hashPassword("password123"));
    }

    @Test
    void differentPasswordsDifferentHashes() {
        assertNotEquals(PasswordUtil.hashPassword("abc"), PasswordUtil.hashPassword("xyz"));
    }

    @Test
    void hashLengthIsSha256() {
        // SHA-256 hex string is always 64 chars
        assertEquals(64, PasswordUtil.hashPassword("anything").length());
    }
}
