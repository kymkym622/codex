package codex.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class WelcomeApp extends Application {
    private final VBox fieldsBox = new VBox(10);
    private final List<TextField> textFields = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        Label heading = new Label("무작위 텍스트 뽑기");
        heading.getStyleClass().add("heading");

        ComboBox<Integer> countSelector = new ComboBox<>();
        countSelector.getItems().addAll(PickerModel.countOptions());
        countSelector.setValue(PickerModel.MIN_COUNT);
        countSelector.setMaxWidth(Double.MAX_VALUE);
        countSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                rebuildTextFields(newValue);
            }
        });

        HBox selectorRow = new HBox(12, new Label("입력칸 개수"), countSelector);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(countSelector, Priority.ALWAYS);

        fieldsBox.setPadding(new Insets(4, 8, 4, 0));

        ScrollPane scrollPane = new ScrollPane(fieldsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("field-scroll");

        Button pickButton = new Button("무작위로 하나 뽑기");
        pickButton.setMaxWidth(Double.MAX_VALUE);
        pickButton.setOnAction(event -> showRandomValue(stage));

        VBox top = new VBox(18, heading, selectorRow);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28, 36, 28, 36));
        root.setTop(top);
        root.setCenter(scrollPane);
        root.setBottom(pickButton);
        BorderPane.setMargin(scrollPane, new Insets(20, 0, 20, 0));

        rebuildTextFields(PickerModel.MIN_COUNT);

        Scene scene = new Scene(root, 520, 560);
        scene.getStylesheets().add(
                WelcomeApp.class.getResource("welcome.css").toExternalForm());

        stage.setTitle("무작위 텍스트 뽑기");
        stage.setMinWidth(440);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();
    }

    private void rebuildTextFields(int count) {
        textFields.clear();
        fieldsBox.getChildren().clear();

        for (int index = 0; index < count; index++) {
            TextField textField = new TextField();
            textField.setPromptText((index + 1) + "번째 값");
            textFields.add(textField);

            Label numberLabel = new Label((index + 1) + ".");
            numberLabel.setMinWidth(28);

            HBox row = new HBox(10, numberLabel, textField);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textField, Priority.ALWAYS);
            fieldsBox.getChildren().add(row);
        }
    }

    private void showRandomValue(Stage owner) {
        List<String> values = textFields.stream().map(TextField::getText).toList();
        String selectedValue = PickerModel.selectRandomValue(
                values,
                ThreadLocalRandom.current());

        Alert result = new Alert(Alert.AlertType.INFORMATION);
        result.initOwner(owner);
        result.setTitle("뽑기 결과");
        result.setHeaderText(null);
        result.setContentText(selectedValue);
        result.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
