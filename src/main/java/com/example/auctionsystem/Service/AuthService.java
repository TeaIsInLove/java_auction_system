package com.example.auctionsystem.Service;

import com.example.auctionsystem.DB.DatabaseManager;
import com.example.auctionsystem.Model.User;
import com.example.auctionsystem.UserRepository.UserRepository;
import com.example.auctionsystem.Util.PasswordUtil;

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

        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(fullName.trim(), normalizedEmail, passwordHash, "USER");
        userRepository.save(newUser);
        return true;
    }

    public boolean login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user.isEmpty()) {
            return false;
        }
        String passwordHash = PasswordUtil.hashPassword(password);
        return user.get().getPassword().equals(passwordHash);
    }
}
