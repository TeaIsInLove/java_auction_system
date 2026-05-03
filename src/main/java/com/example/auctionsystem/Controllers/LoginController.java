package com.example.auctionsystem.Controllers;

import com.example.auctionsystem.Model.UserService;
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

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (UserService.login(user, pass)) {
            messageLabel.setText("Đăng nhập thành công");

            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/auctionsystem/Views/List.fxml")
                );
                Parent root = loader.load();

                    Stage stage = (Stage) usernameField.getScene().getWindow();
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