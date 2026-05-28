package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.DatabaseManager;
import com.example.bai_tap_lon.auth.UserRepository;
import com.example.bai_tap_lon.model.auction.Auction;
import com.example.bai_tap_lon.session.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ListController {
    @FXML private TableView<AppUser> tableView;
    @FXML private TableColumn<AppUser, String> usernameCol;
    @FXML private TableColumn<AppUser, String> emailCol;
    @FXML private TableColumn<AppUser, String> roleCol;
    @FXML private TableColumn<AppUser, String> balanceCol;
    @FXML private TableColumn<AppUser, Void> actionsCol;
    @FXML private Button pendingApprovalBtn;

    private final UserRepository userRepository = new UserRepository(new DatabaseManager());
    private final AuctionWorkspace workspace = AuctionWorkspace.getInstance();
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
    public void handleApprovePendingAuctions() {
        workspace.initialize();
        List<Auction> pending = workspace.getPendingApprovalAuctions();

        Stage dialog = new Stage();
        dialog.setTitle("Duyệt phiên đấu giá");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(tableView.getScene().getWindow());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f7fb;");

        Label titleLabel = new Label("Phiên đấu giá chờ duyệt (" + pending.size() + ")");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.getChildren().add(titleLabel);

        if (pending.isEmpty()) {
            root.getChildren().add(new Label("Không có phiên nào chờ duyệt."));
        } else {
            ScrollPane scroll = new ScrollPane();
            VBox list = new VBox(10);
            list.setPadding(new Insets(8));

            for (Auction auction : pending) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dbe3ef; -fx-border-radius: 8;");

                VBox info = new VBox(4);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
                String sellerName = auction.getSeller() != null ? auction.getSeller().getUsername() : "N/A";
                info.getChildren().addAll(
                    new Label(auction.getItem().getName() + " [" + auction.getItem().getItemCategory() + "]"),
                    new Label("Người tạo: " + sellerName + " | Giá khởi điểm: " + AuctionWorkspace.formatVnd(auction.getItem().getStartingPrice()) + " đ"),
                    new Label("Tạo lúc: " + (auction.getCreatedAt() != null ? auction.getCreatedAt().toString().substring(0, 16) : "N/A"))
                );
                info.getChildren().forEach(n -> ((Label) n).setStyle("-fx-font-size: 12px;"));
                ((Label) info.getChildren().get(0)).setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                Button approveBtn = new Button("✔ Duyệt");
                approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");

                Button rejectBtn = new Button("✘ Từ chối");
                rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");

                String adminName = SessionManager.getInstance().getUsername();
                approveBtn.setOnAction(e -> {
                    workspace.approveAuction(auction.getId(), adminName);
                    list.getChildren().remove(row);
                    titleLabel.setText("Phiên đấu giá chờ duyệt (" + workspace.getPendingApprovalAuctions().size() + ")");
                });
                rejectBtn.setOnAction(e -> {
                    workspace.deleteAuction(auction, adminName, true);
                    list.getChildren().remove(row);
                    titleLabel.setText("Phiên đấu giá chờ duyệt (" + workspace.getPendingApprovalAuctions().size() + ")");
                });

                row.getChildren().addAll(info, approveBtn, rejectBtn);
                list.getChildren().add(row);
            }

            scroll.setContent(list);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(400);
            root.getChildren().add(scroll);
        }

        Button closeBtn = new Button("Đóng");
        closeBtn.setOnAction(e -> dialog.close());
        root.getChildren().add(closeBtn);

        dialog.setScene(new Scene(root, 700, 500));
        dialog.showAndWait();
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
