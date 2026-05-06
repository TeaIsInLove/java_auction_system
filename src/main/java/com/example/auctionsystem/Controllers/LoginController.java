package com.example.auctionsystem.Controllers;

import com.example.auctionsystem.Model.User;
import com.example.auctionsystem.Service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();
    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        User currentUser = null;
        if ("admin".equalsIgnoreCase(user) && "123".equals(pass)) {
            currentUser = new User("admin", pass, "", "ADMIN");
        } else if (authService.login(user, pass)) {
            currentUser = new User(user, pass, "USER", "USER");
        }

        if (currentUser != null) {

            try {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Parent root;

                if ("ADMIN".equals(currentUser.getRole())) {
                    // 👉 ADMIN
                    root = FXMLLoader.load(
                            getClass().getResource("/com/example/auctionsystem/Views/List.fxml")
                    );
                    messageLabel.setText("Đăng nhập Admin thành công");

                } else {
                    // 👉 USER
                    root = FXMLLoader.load(
                            getClass().getResource("/com/example/auctionsystem/Views/Auction.fxml")
                    );
                    messageLabel.setText("Đăng nhập User thành công");
                }

                stage.setScene(new Scene(root));

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            messageLabel.setText("Sai tài khoản hoặc mật khẩu");
        }
    }
    @FXML
    public void goToRegister(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/example/auctionsystem/Views/register.fxml")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}