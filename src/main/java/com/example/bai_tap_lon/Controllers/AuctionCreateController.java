package com.example.bai_tap_lon.Controllers;

import com.example.bai_tap_lon.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AuctionCreateController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private TextField itemNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField sellerField;
    @FXML private TextField extraInfoField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;

    private AuctionWorkspace workspace;

    @FXML
    public void initialize() {
        itemTypeCombo.setItems(FXCollections.observableArrayList("electronics", "art", "vehicle"));
        itemTypeCombo.getSelectionModel().selectFirst();
        itemTypeCombo.valueProperty().addListener((obs, oldValue, type) -> updateExtraInfoPrompt(type));
        updateExtraInfoPrompt(itemTypeCombo.getValue());

        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today.plusDays(7));
        startTimeField.setText("09:00");
        endTimeField.setText("18:00");
        if (SessionManager.getInstance().isLoggedIn()) {
            sellerField.setText(SessionManager.getInstance().getUsername());
            sellerField.setEditable(false);
        } else {
            sellerField.setText("Default Seller");
            sellerField.setEditable(true);
        }
    }

    public void setWorkspace(AuctionWorkspace workspace) {
        this.workspace = workspace;
        if (SessionManager.getInstance().isLoggedIn()) {
            sellerField.setText(SessionManager.getInstance().getUsername());
            sellerField.setEditable(false);
        }
    }

    @FXML
    private void handleCreateAuction() {
        if (!SessionManager.getInstance().isLoggedIn()
                || "Guest".equalsIgnoreCase(SessionManager.getInstance().getUsername())) {
            workspace.showMessage("Vui long dang nhap de tao phien dau gia.", false);
            return;
        }

        String type = itemTypeCombo.getValue();
        String itemName = text(itemNameField);
        String seller = SessionManager.getInstance().getUsername();
        String extraInfo = text(extraInfoField);

        if (type == null || itemName.isEmpty()) {
            workspace.showMessage("Nhap day du loai san pham va ten san pham.", false);
            return;
        }

        Double startingPrice = parseMoney(startingPriceField.getText());
        if (startingPrice == null || startingPrice <= 0) {
            workspace.showMessage("Gia khoi diem phai la so lon hon 0.", false);
            return;
        }

        LocalDateTime startTime = parseDateTime(startDatePicker, startTimeField, "thoi gian bat dau");
        LocalDateTime endTime = parseDateTime(endDatePicker, endTimeField, "thoi gian ket thuc");
        if (startTime == null || endTime == null) {
            return;
        }
        if (!endTime.isAfter(startTime)) {
            workspace.showMessage("Thoi gian ket thuc phai sau thoi gian bat dau.", false);
            return;
        }

        workspace.createAuction(type, itemName, descriptionArea.getText(), startingPrice, startTime, endTime, seller, extraInfo);
        clearCreateForm();
    }

    @FXML
    private void handleClearForm() {
        clearCreateForm();
        workspace.showMessage("Da xoa form tao phien.", true);
    }

    private LocalDateTime parseDateTime(DatePicker picker, TextField timeField, String fieldName) {
        LocalDate date = picker.getValue();
        if (date == null) {
            workspace.showMessage("Chon ngay cho " + fieldName + ".", false);
            return null;
        }

        try {
            LocalTime time = LocalTime.parse(text(timeField), TIME_FORMATTER);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            workspace.showMessage("Nhap " + fieldName + " theo dinh dang HH:mm.", false);
            return null;
        }
    }

    private Double parseMoney(String value) {
        try {
            return Double.parseDouble(value.trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private void clearCreateForm() {
        itemNameField.clear();
        descriptionArea.clear();
        startingPriceField.clear();
        extraInfoField.clear();
        itemTypeCombo.getSelectionModel().selectFirst();
        if (SessionManager.getInstance().isLoggedIn()) {
            sellerField.setText(SessionManager.getInstance().getUsername());
        }
    }

    private void updateExtraInfoPrompt(String type) {
        if ("art".equals(type)) {
            extraInfoField.setPromptText("Tac gia");
        } else if ("vehicle".equals(type)) {
            extraInfoField.setPromptText("Hang xe");
        } else {
            extraInfoField.setPromptText("Thuong hieu");
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
