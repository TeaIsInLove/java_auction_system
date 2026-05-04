package com.example.auctionsystem.Controllers;

import com.example.auctionsystem.Model.UserService;
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
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private UserService userService = new UserService();

    @FXML
    public void handleRegister(ActionEvent event) {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            userService.register(username, password);

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

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/example/auctionsystem/Views/login.fxml")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}