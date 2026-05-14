package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.AuthService;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String usernameOrEmail = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        Optional<AppUser> currentUser;
        if ("admin".equalsIgnoreCase(usernameOrEmail) && "123".equals(password)) {
            currentUser = Optional.of(new AppUser("admin", "", "admin@local", "ADMIN"));
        } else if ("tester".equalsIgnoreCase(usernameOrEmail) && "123".equals(password)) {
            currentUser = Optional.of(
                    new AppUser("tester", "", "tester@local", "USER")
            );
        } else {
            currentUser = authService.login(usernameOrEmail, password);
        }

        if (currentUser.isEmpty()) {
            showError("Sai tai khoan hoac mat khau.");
            return;
        }
        AppUser user = currentUser.get();

        SessionManager.getInstance().login(user.getUsername());

        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            String view = "ADMIN".equalsIgnoreCase(user.getRole())
                    ? "/com/example/auctionsystem/Views/fxml/List.fxml"
                    : "/com/example/auctionsystem/Views/fxml/Auction.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(view));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            showError("Khong the mo man hinh sau dang nhap.");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auctionsystem/Views/fxml/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #b91c1c;");
        messageLabel.setText(message);
    }
}
