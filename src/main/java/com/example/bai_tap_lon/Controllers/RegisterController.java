package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AuthService;
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

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleRegister(ActionEvent event) {
        try {
            String username = text(usernameField);
            String email = text(emailField);
            String password = passwordField.getText() == null ? "" : passwordField.getText();
            String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

            if (!password.equals(confirmPassword)) {
                showError("Mat khau xac nhan khong khop.");
                return;
            }

            boolean registered = authService.register(username, email, password);
            if (!registered) {
                showError("Email da ton tai!");
                return;
            }

            goToLogin(event);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auctionsystem/Views/fxml/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #b91c1c;");
        messageLabel.setText(message);
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
