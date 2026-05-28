package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.Services.DashboardService;
import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.PasswordUtil;
import com.example.bai_tap_lon.auth.UserRepository;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class ProfileController {

    private final DashboardService dashboardService = new DashboardService();
    private final UserRepository userRepository = new UserRepository();

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label balanceLabel;
    @FXML private Label createdAuctionsLabel;
    @FXML private Label roleLabel;

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        AppUser user = SessionManager.getInstance().getCurrentUser();

        if (user == null) {
            showError("Khong the tai thong tin nguoi dung.");
            return;
        }

        usernameLabel.setText(user.getUsername());
        emailLabel.setText(user.getEmail());
        roleLabel.setText(user.getRole());

        double freshBalance = userRepository.findByEmail(user.getEmail())
                .map(AppUser::getBalance)
                .orElse(user.getBalance());
        balanceLabel.setText(AuctionWorkspace.formatVnd(freshBalance) + " VND");

        int createdCount = dashboardService.getCreatedAuctionCount(user.getUsername());
        createdAuctionsLabel.setText(String.valueOf(createdCount));
    }

    @FXML
    private void handleDeposit() {
        AppUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Nạp tiền vào tài khoản");
        dialog.setHeaderText("Nhập số tiền muốn nạp (VND)");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField amountField = new TextField();
        amountField.setPromptText("Ví dụ: 1000000");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(amountField, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return Double.parseDouble(amountField.getText().trim().replace(",", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            if (amount <= 0) {
                showError("Số tiền nạp phải lớn hơn 0.");
                return;
            }

            double currentBalance = userRepository.findByEmail(user.getEmail())
                    .map(AppUser::getBalance)
                    .orElse(0.0);
            double newBalance = currentBalance + amount;

            userRepository.updateBalanceByEmail(user.getEmail(), newBalance);
            loadUserProfile();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText(null);
            ok.setContentText("Nạp tiền thành công! Số dư mới: " + AuctionWorkspace.formatVnd(newBalance) + " VND");
            ok.showAndWait();
        });
    }

    @FXML
    private void handleChangePassword() {
        AppUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText("Nhập mật khẩu mới");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField currentField = new PasswordField();
        currentField.setPromptText("Mật khẩu hiện tại");
        PasswordField newField = new PasswordField();
        newField.setPromptText("Mật khẩu mới (ít nhất 6 ký tự)");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Xác nhận mật khẩu mới");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("Mật khẩu hiện tại:"), 0, 0);
        grid.add(currentField, 1, 0);
        grid.add(new Label("Mật khẩu mới:"), 0, 1);
        grid.add(newField, 1, 1);
        grid.add(new Label("Xác nhận:"), 0, 2);
        grid.add(confirmField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> null);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String currentPwd = currentField.getText();
                String newPwd = newField.getText();
                String confirmPwd = confirmField.getText();

                String storedHash = userRepository.findByEmail(user.getEmail())
                        .map(AppUser::getPassword)
                        .orElse("");
                if (!PasswordUtil.hashPassword(currentPwd).equals(storedHash)) {
                    showError("Mật khẩu hiện tại không đúng.");
                    event.consume();
                    return;
                }
                if (newPwd.length() < 6) {
                    showError("Mật khẩu mới phải có ít nhất 6 ký tự.");
                    event.consume();
                    return;
                }
                if (!newPwd.equals(confirmPwd)) {
                    showError("Mật khẩu xác nhận không khớp.");
                    event.consume();
                    return;
                }

                userRepository.updatePassword(user.getEmail(), PasswordUtil.hashPassword(newPwd));

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setHeaderText(null);
                ok.setContentText("Đổi mật khẩu thành công!");
                ok.showAndWait();
            }
        );

        dialog.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {
        usernameLabel.getScene().getWindow().hide();
    }
}
