module com.example.bai_tap_lon {
    requires javafx.controls;
    requires javafx.fxml;
    // ... các requires khác nếu có ...

    // Thêm dòng này để xin phép dùng Database:
    requires java.sql;

    opens com.example.bai_tap_lon to javafx.fxml;
    exports com.example.bai_tap_lon;
}