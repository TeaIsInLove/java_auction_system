package com.example.auctionsystem.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {
    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                builder.append(String.format("%02x", hashedByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Khong the bam mat khau.", ex);
        }
    }
}
