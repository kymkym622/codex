package codex.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import codex.app.split.ExcelSchemaReader;
import codex.app.split.MysqlGenerator;
import codex.app.split.OracleGenerator;
import codex.app.split.PostgreSqlGenerator;
import codex.app.split.SchemaModel.TableDefinition;
import codex.app.split.SqlGenerator;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class Main extends Application {
    private final ComboBox<DatabaseType> dbSelector = new ComboBox<>();
    private final TextArea resultArea = new TextArea();
    private final Label fileLabel = new Label("선택된 엑셀 파일이 없습니다.");
    private final Label statusLabel = new Label("엑셀 파일과 DB 종류를 선택하세요.");
    private final Button generateButton = new Button("CREATE문 생성");
    private final Button copyButton = new Button("복사");
    private final Button saveButton = new Button("TXT 저장");
    private File selectedExcel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Label title = new Label("DB CREATE문 생성기");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label description = new Label("엑셀의 테이블 정보를 읽어 MySQL / Oracle / PostgreSQL용 CREATE문을 생성합니다.");

        Button fileButton = new Button("엑셀 파일 선택");
        fileButton.setOnAction(event -> chooseExcel(stage));

        dbSelector.getItems().setAll(DatabaseType.values());
        dbSelector.setPromptText("DB 종류 선택");
        dbSelector.setPrefWidth(180);
        dbSelector.valueProperty().addListener((obs, oldValue, newValue) -> updateGenerateState());

        fileLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileLabel, Priority.ALWAYS);
        HBox fileRow = new HBox(10, fileButton, fileLabel);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        generateButton.setDisable(true);
        generateButton.setOnAction(event -> generate());
        HBox optionRow = new HBox(10, new Label("DB 종류"), dbSelector, generateButton);
        optionRow.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(12, fileRow, optionRow, statusLabel);
        controls.setPadding(new Insets(18));

        resultArea.setEditable(false);
        resultArea.setWrapText(false);
        resultArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        copyButton.setDisable(true);
        copyButton.setOnAction(event -> copyResult());
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> saveResult(stage));

        HBox resultButtons = new HBox(10, copyButton, saveButton);
        resultButtons.setAlignment(Pos.CENTER_RIGHT);
        VBox center = new VBox(10, new Label("생성 결과"), resultArea, resultButtons);
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setTop(new VBox(18, new VBox(5, title, description), controls));
        root.setCenter(center);
        BorderPane.setMargin(center, new Insets(18, 0, 0, 0));

        stage.setScene(new Scene(root, 1000, 760));
        stage.setTitle("DB CREATE문 생성기");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    private void chooseExcel(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 파일 (*.xlsx, *.xls)", "*.xlsx", "*.xls"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        selectedExcel = file;
        fileLabel.setText(file.getAbsolutePath());
        resultArea.clear();
        setResultActions(false);
        updateGenerateState();
    }

    private void updateGenerateState() {
        generateButton.setDisable(selectedExcel == null || dbSelector.getValue() == null);
    }

    private void generate() {
        try {
            DatabaseType db = dbSelector.getValue();
            List<TableDefinition> tables = ExcelSchemaReader.read(selectedExcel);
            if (tables.isEmpty()) throw new IllegalArgumentException("CREATE문으로 만들 테이블 시트를 찾지 못했습니다.");
            SqlGenerator generator = switch (db) {
                case MYSQL -> new MysqlGenerator();
                case ORACLE -> new OracleGenerator();
                case POSTGRESQL -> new PostgreSqlGenerator();
            };
            String result = generator.generate(tables);
            resultArea.setText(result);
            resultArea.positionCaret(0);
            setResultActions(!result.isBlank());
            statusLabel.setText(tables.size() + "개 테이블의 " + db + " CREATE문을 생성했습니다.");
        } catch (Exception e) {
            resultArea.clear();
            setResultActions(false);
            showError(e.getMessage());
        }
    }

    private void copyResult() {
        ClipboardContent content = new ClipboardContent();
        content.putString(resultArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void saveResult(Stage stage) {
        if (resultArea.getText().isBlank()) return;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("텍스트 파일 (*.txt)", "*.txt"));
        chooser.setInitialFileName("create_statements_" + dbSelector.getValue().name().toLowerCase() + ".txt");
        File target = chooser.showSaveDialog(stage);
        if (target == null) return;
        try {
            Files.writeString(target.toPath(), resultArea.getText(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void setResultActions(boolean enabled) {
        copyButton.setDisable(!enabled);
        saveButton.setDisable(!enabled);
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("CREATE문 생성 실패");
        alert.setContentText(message == null ? "알 수 없는 오류" : message);
        alert.showAndWait();
    }

    private enum DatabaseType {
        MYSQL("MySQL"), ORACLE("Oracle"), POSTGRESQL("PostgreSQL");
        private final String label;
        DatabaseType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}
