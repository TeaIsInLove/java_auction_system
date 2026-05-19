package com.example.bai_tap_lon.session;

import com.example.bai_tap_lon.auth.AppUser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SessionManager {

    // Singleton instance
    private static final SessionManager INSTANCE =
            new SessionManager();

    // User hiện tại
    private final ObjectProperty<AppUser> currentUser =
            new SimpleObjectProperty<>();

    // Constructor private
    private SessionManager() {
    }

    // Lấy instance
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    // Property cho JavaFX binding
    public ObjectProperty<AppUser> currentUserProperty() {
        return currentUser;
    }

    // Lấy user hiện tại
    public AppUser getCurrentUser() {
        return currentUser.get();
    }

    // Đăng nhập
    public void login(AppUser user) {
        currentUser.set(user);
    }

    // Đăng xuất
    public void logout() {
        currentUser.set(null);
    }

    // Kiểm tra login
    public boolean isLoggedIn() {
        return currentUser.get() != null;
    }

    // Helper lấy username
    public String getUsername() {
        return isLoggedIn()
                ? currentUser.get().getUsername()
                : "Guest";
    }

    // Helper lấy role
    public String getRole() {
        return isLoggedIn()
                ? currentUser.get().getRole()
                : "GUEST";
    }

    // Kiểm tra admin
    public boolean isAdmin() {
        return isLoggedIn() && "ADMIN".equalsIgnoreCase(currentUser.get().getRole());
    }
}