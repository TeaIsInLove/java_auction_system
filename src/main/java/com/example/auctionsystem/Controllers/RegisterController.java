package com.example.auctionsystem.Controllers;

import com.example.auctionsystem.Service.AuthService;
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

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private AuthService authService = new AuthService();

    @FXML
    public void handleRegister(ActionEvent event) {
        try {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showError("Vui long nhap day du thong tin dang ky.");
                return;
            }

            if (!email.contains("@")) {
                showError("Email khong hop le.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showError("Mat khau xac nhan khong khop.");
                return;
            }

            boolean registered = authService.register(username, email, password);
            if (!registered) {
                showError("Email da ton tai!");
                return;
            }

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Đăng ký thành công!");

            //  chuyển về login
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/auctionsystem/Views/login.fxml")
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(message);
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/example/auctionsystem/Views/login.fxml")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}