package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.auth.AppUser;
import com.example.bai_tap_lon.auth.DatabaseManager;
import com.example.bai_tap_lon.auth.UserRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class ListController {
    @FXML private TableView<AppUser> tableView;
    @FXML private TableColumn<AppUser, String> usernameCol;
    @FXML private TableColumn<AppUser, String> emailCol;
    @FXML private TableColumn<AppUser, String> roleCol;

    private final UserRepository userRepository = new UserRepository(new DatabaseManager());

    @FXML
    public void initialize() {
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));

        tableView.getItems().setAll(userRepository.findAll());
    }

    @FXML
    public void handleOpenDashboard() {
        try {
            Stage stage = (Stage) tableView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auctionsystem/Views/fxml/Auction.fxml"));
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
