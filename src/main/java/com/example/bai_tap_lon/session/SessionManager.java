package com.example.bai_tap_lon.session;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final StringProperty currentUsername =
            new SimpleStringProperty("Guest");

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public StringProperty currentUsernameProperty() {
        return currentUsername;
    }

    public String getCurrentUsername() {
        return currentUsername.get();
    }

    public void login(String username) {
        currentUsername.set(username);
    }

    public void logout() {
        currentUsername.set("Guest");
    }
}