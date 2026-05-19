module com.example.auctionsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires eu.hansolo.fx.countries;
    requires com.almasb.fxgl.all;
    requires java.naming;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.example.bai_tap_lon.Controllers to javafx.fxml;
    exports com.example.bai_tap_lon;
    exports com.example.bai_tap_lon.network;
}