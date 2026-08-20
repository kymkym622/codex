module codex {
    requires javafx.controls;
    requires java.net.http;

    exports codex.app;
    opens codex.app to javafx.graphics;
}
