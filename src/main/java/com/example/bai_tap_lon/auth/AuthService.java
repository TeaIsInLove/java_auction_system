package com.example.bai_tap_lon.auth;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository(new DatabaseManager());
    }

    public boolean register(String fullName, String email, String password) {
        if (fullName == null || fullName.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap day du thong tin dang ky.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email khong hop le.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mat khau phai >= 6 ky tu!");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            return false;
        }

        double initialBalance = 100_000_000.0; // 100 triệu VND
        AppUser user = new AppUser(fullName.trim(), PasswordUtil.hashPassword(password), normalizedEmail, "USER", initialBalance);
        userRepository.save(user);
        return true;
    }

    public Optional<AppUser> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        Optional<AppUser> user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user.isEmpty()) {
            return Optional.empty();
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        return passwordHash.equals(user.get().getPassword()) ? user : Optional.empty();
    }
}
