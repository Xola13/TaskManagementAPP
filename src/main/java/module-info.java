module TaskManagementApp {
    requires java.sql;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires java.base;

    exports org.example;
}