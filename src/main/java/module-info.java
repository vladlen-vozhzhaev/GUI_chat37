module com.example.guichat37 {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.guichat37 to javafx.fxml;
    exports com.example.guichat37;
}