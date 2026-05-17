package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.Services.DashboardService;
import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.UserRepository;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

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

        // Load fresh balance from database instead of using cached AppUser
        double freshBalance = userRepository.findByEmail(user.getEmail())
                .map(AppUser::getBalance)
                .orElse(user.getBalance());
        balanceLabel.setText(AuctionWorkspace.formatVnd(freshBalance) + " VND");

        int createdCount = dashboardService.getCreatedAuctionCount(user.getUsername());
        createdAuctionsLabel.setText(String.valueOf(createdCount));
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
