package codex.app;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class WelcomeApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new Group(), 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
