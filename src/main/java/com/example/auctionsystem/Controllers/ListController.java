package com.example.auctionsystem.Controllers;

import com.example.auctionsystem.DB.DatabaseManager;
import com.example.auctionsystem.Model.User;
import com.example.auctionsystem.Service.AuthService;
import com.example.auctionsystem.UserRepository.UserRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ListController {

    @FXML
    private TableView<User> tableView;

    @FXML
    private TableColumn<User, String> usernameCol;

    @FXML
    private TableColumn<User, String> passwordCol;

    private final UserRepository userRepository = new UserRepository(new DatabaseManager());

    @FXML
    public void initialize() {
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        passwordCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassword()));

        tableView.getItems().addAll(userRepository.findAll());
    }
}