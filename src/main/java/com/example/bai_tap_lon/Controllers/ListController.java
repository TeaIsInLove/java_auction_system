package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.DatabaseManager;
import com.example.bai_tap_lon.auth.UserRepository;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public class ListController {
    @FXML private TableView<AppUser> tableView;
    @FXML private TableColumn<AppUser, String> usernameCol;
    @FXML private TableColumn<AppUser, String> emailCol;
    @FXML private TableColumn<AppUser, String> roleCol;
    @FXML private TableColumn<AppUser, String> balanceCol;
    @FXML private TableColumn<AppUser, Void> actionsCol;

    private final UserRepository userRepository = new UserRepository(new DatabaseManager());
    private static final DecimalFormat VND_FMT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        VND_FMT = new DecimalFormat("#,###", sym);
    }

    @FXML
    public void initialize() {
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        balanceCol.setCellValueFactory(data ->
                new SimpleStringProperty(VND_FMT.format((long) data.getValue().getBalance()) + " đ"));

        setupActionsColumn();
        loadUsers();
    }

    private void loadUsers() {
        tableView.getItems().setAll(userRepository.findAll());
    }

    private void setupActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button promoteBtn = new Button("Đổi vai trò");
            private final Button deleteBtn  = new Button("Xóa");
            private final HBox box = new HBox(6, promoteBtn, deleteBtn);

            {
                box.setPadding(new Insets(2, 0, 2, 0));

                promoteBtn.setStyle("""
                        -fx-background-color: #3b82f6;
                        -fx-text-fill: white;
                        -fx-font-size: 11px;
                        -fx-padding: 4 10 4 10;
                        -fx-background-radius: 6;
                        -fx-cursor: hand;
                        """);

                deleteBtn.setStyle("""
                        -fx-background-color: #ef4444;
                        -fx-text-fill: white;
                        -fx-font-size: 11px;
                        -fx-padding: 4 10 4 10;
                        -fx-background-radius: 6;
                        -fx-cursor: hand;
                        """);

                promoteBtn.setOnAction(e -> {
                    AppUser user = getTableView().getItems().get(getIndex());
                    handleToggleRole(user);
                });

                deleteBtn.setOnAction(e -> {
                    AppUser user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppUser user = getTableView().getItems().get(getIndex());
                    // Prevent admin from deleting or demoting themselves
                    String me = SessionManager.getInstance().getEmail();
                    boolean isSelf = user.getEmail().equals(me);
                    deleteBtn.setDisable(isSelf);
                    promoteBtn.setDisable(isSelf);
                    setGraphic(box);
                }
            }
        });
    }

    private void handleToggleRole(AppUser user) {
        String currentRole = user.getRole();
        String newRole = "ADMIN".equalsIgnoreCase(currentRole) ? "USER" : "ADMIN";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đổi vai trò");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Đổi vai trò của \"" + user.getUsername() + "\" từ " + currentRole + " → " + newRole + "?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean ok = userRepository.updateRole(user.getEmail(), newRole);
            if (ok) {
                loadUsers();
            } else {
                showError("Không thể cập nhật vai trò.");
            }
        }
    }

    private void handleDelete(AppUser user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa tài khoản \"" + user.getUsername() + "\" (" + user.getEmail() + ")?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean ok = userRepository.deleteByEmail(user.getEmail());
            if (ok) {
                loadUsers();
            } else {
                showError("Không tìm thấy tài khoản hoặc xóa thất bại.");
            }
        }
    }

    @FXML
    public void handleRefresh() {
        loadUsers();
    }

    @FXML
    public void handleOpenDashboard() {
        try {
            Stage stage = (Stage) tableView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/auctionsystem/Views/fxml/Auction.fxml"));
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
