module codex {
    requires javafx.controls;
    requires java.net.http;
    opens codex.app to javafx.graphics;
}
