package codex.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class WelcomeApp {
    private WelcomeApp() {
    }

    public static void main(String[] args) {
        Application.launch(FxApplication.class, args);
    }

    public static final class FxApplication extends Application {
        private final ComboBox<DatabaseType> dbSelector = new ComboBox<>();
        private final TextArea resultArea = new TextArea();
        private final Label fileLabel = new Label("선택된 엑셀 파일이 없습니다.");
        private final Label statusLabel = new Label("엑셀 파일과 DB 종류를 선택하세요.");
        private final Button generateButton = new Button("CREATE문 생성");
        private final Button copyButton = new Button("복사");
        private final Button saveButton = new Button("TXT 저장");

        private File selectedExcel;

        @Override
        public void start(Stage stage) {
            Label title = new Label("DB CREATE문 생성기");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            Label description = new Label(
                    "엑셀의 테이블 정보를 읽어 MySQL / Oracle / MSSQL용 CREATE문을 생성합니다.");
            description.setStyle("-fx-text-fill: #555555;");

            Button fileButton = new Button("엑셀 파일 선택");
            fileButton.setOnAction(event -> chooseExcel(stage));

            dbSelector.getItems().setAll(DatabaseType.values());
            dbSelector.setPromptText("DB 종류 선택");
            dbSelector.setPrefWidth(180);
            dbSelector.valueProperty().addListener((obs, oldValue, newValue) -> updateGenerateState());

            fileLabel.setMaxWidth(Double.MAX_VALUE);
            fileLabel.setStyle("-fx-text-fill: #444444;");
            HBox.setHgrow(fileLabel, Priority.ALWAYS);

            HBox fileRow = new HBox(10, fileButton, fileLabel);
            fileRow.setAlignment(Pos.CENTER_LEFT);

            generateButton.setDisable(true);
            generateButton.setOnAction(event -> generate());

            HBox optionRow = new HBox(10, new Label("DB 종류"), dbSelector, generateButton);
            optionRow.setAlignment(Pos.CENTER_LEFT);

            VBox controls = new VBox(12, fileRow, optionRow, statusLabel);
            controls.setPadding(new Insets(18));
            controls.setStyle(
                    "-fx-background-color: #f7f7f7; -fx-background-radius: 8; -fx-border-color: #dddddd; -fx-border-radius: 8;");

            resultArea.setEditable(false);
            resultArea.setWrapText(false);
            resultArea.setPromptText("생성된 CREATE문이 여기에 표시됩니다.");
            resultArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

            copyButton.setDisable(true);
            copyButton.setTooltip(new Tooltip("생성 결과 전체를 클립보드에 복사합니다."));
            copyButton.setOnAction(event -> copyResult());

            saveButton.setDisable(true);
            saveButton.setOnAction(event -> saveResult(stage));

            HBox resultButtons = new HBox(10, copyButton, saveButton);
            resultButtons.setAlignment(Pos.CENTER_RIGHT);

            VBox center = new VBox(10, new Label("생성 결과"), resultArea, resultButtons);
            VBox.setVgrow(resultArea, Priority.ALWAYS);

            VBox header = new VBox(5, title, description);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(24));
            root.setTop(new VBox(18, header, controls));
            root.setCenter(center);
            BorderPane.setMargin(center, new Insets(18, 0, 0, 0));

            Scene scene = new Scene(root, 1000, 760);
            stage.setTitle("DB CREATE문 생성기");
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.setScene(scene);
            stage.show();
        }

        private void chooseExcel(Stage stage) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("테이블 정보 엑셀 선택");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel 파일 (*.xlsx, *.xls)", "*.xlsx", "*.xls"));
            File file = chooser.showOpenDialog(stage);
            if (file == null) {
                return;
            }

            selectedExcel = file;
            fileLabel.setText(file.getAbsolutePath());
            statusLabel.setText("파일을 선택했습니다. DB 종류를 선택한 뒤 CREATE문을 생성하세요.");
            resultArea.clear();
            setResultActions(false);
            updateGenerateState();
        }

        private void updateGenerateState() {
            generateButton.setDisable(selectedExcel == null || dbSelector.getValue() == null);
        }

        private void generate() {
            try {
                DatabaseType databaseType = dbSelector.getValue();
                List<TableDefinition> tables = ExcelSchemaReader.read(selectedExcel);
                if (tables.isEmpty()) {
                    throw new IllegalArgumentException("CREATE문으로 만들 테이블 시트를 찾지 못했습니다.");
                }

                String result = SqlGenerator.generate(tables, databaseType);
                resultArea.setText(result);
                resultArea.positionCaret(0);
                setResultActions(!result.isBlank());
                statusLabel.setText(tables.size() + "개 테이블의 " + databaseType + " CREATE문을 생성했습니다.");
            } catch (Exception exception) {
                resultArea.clear();
                setResultActions(false);
                statusLabel.setText("생성 실패");
                showError("CREATE문 생성 실패", rootMessage(exception));
            }
        }

        private void copyResult() {
            if (resultArea.getText().isBlank()) {
                return;
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(resultArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("생성 결과를 클립보드에 복사했습니다.");
        }

        private void saveResult(Stage stage) {
            if (resultArea.getText().isBlank()) {
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("CREATE문 TXT 저장");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("텍스트 파일 (*.txt)", "*.txt"));
            DatabaseType db = dbSelector.getValue();
            String suffix = db == null ? "sql" : db.name().toLowerCase(Locale.ROOT);
            chooser.setInitialFileName("create_statements_" + suffix + ".txt");
            File target = chooser.showSaveDialog(stage);
            if (target == null) {
                return;
            }

            try {
                Files.writeString(target.toPath(), resultArea.getText(), StandardCharsets.UTF_8);
                statusLabel.setText("TXT 파일을 저장했습니다: " + target.getAbsolutePath());
            } catch (IOException exception) {
                showError("TXT 저장 실패", exception.getMessage());
            }
        }

        private void setResultActions(boolean enabled) {
            copyButton.setDisable(!enabled);
            saveButton.setDisable(!enabled);
        }

        private static void showError(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message == null || message.isBlank() ? "알 수 없는 오류가 발생했습니다." : message);
            alert.showAndWait();
        }

        private static String rootMessage(Throwable throwable) {
            Throwable current = throwable;
            while (current.getCause() != null) {
                current = current.getCause();
            }
            return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
        }
    }

    private enum DatabaseType {
        MYSQL("MySQL"),
        ORACLE("Oracle"),
        MSSQL("MSSQL");

        private final String label;

        DatabaseType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record ColumnDefinition(
            int sequence,
            String name,
            String dataType,
            String size,
            String scale,
            boolean nullable,
            String defaultValue,
            Integer primaryKeyOrder,
            boolean unique,
            String description) {
    }

    private record TableDefinition(String name, String description, List<ColumnDefinition> columns) {
    }

    private static final class ExcelSchemaReader {
        private static final DataFormatter FORMATTER = new DataFormatter(Locale.KOREA);

        private ExcelSchemaReader() {
        }

        static List<TableDefinition> read(File file) throws IOException {
            List<TableDefinition> tables = new ArrayList<>();
            try (Workbook workbook = WorkbookFactory.create(file)) {
                for (Sheet sheet : workbook) {
                    if (sheet.getSheetName().equalsIgnoreCase("목록") || sheet.getSheetName().equalsIgnoreCase("index")) {
                        continue;
                    }
                    TableDefinition table = parseSheet(sheet);
                    if (table != null && !table.columns().isEmpty()) {
                        tables.add(table);
                    }
                }
            }
            return tables;
        }

        private static TableDefinition parseSheet(Sheet sheet) {
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex < 0) {
                return null;
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            Map<String, Integer> indexes = headerIndexes(headerRow);
            int nameIndex = requiredIndex(indexes, "컬럼명");
            int typeIndex = requiredIndex(indexes, "데이터 타입");

            String tableName = sheet.getSheetName().trim();
            String tableDescription = cellText(sheet.getRow(0), 0);
            if (isBlankOrPlaceholder(tableDescription)) {
                tableDescription = tableName;
            }

            List<ColumnDefinition> columns = new ArrayList<>();
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String columnName = cellText(row, nameIndex);
                String dataType = cellText(row, typeIndex);
                if (columnName.isBlank() || dataType.isBlank()) {
                    continue;
                }

                int sequence = parseInt(cellText(row, indexes.getOrDefault("순번", -1)), columns.size() + 1);
                String size = cellText(row, indexes.getOrDefault("크기", -1));
                String scale = cellText(row, indexes.getOrDefault("소수점", -1));
                String nullableText = cellText(row, indexes.getOrDefault("NULL 허용", -1));
                boolean nullable = !nullableText.equalsIgnoreCase("N") && !nullableText.equalsIgnoreCase("NO");
                String defaultValue = normalizeOptional(cellText(row, indexes.getOrDefault("기본값", -1)));
                Integer pkOrder = parseNullableInt(cellText(row, indexes.getOrDefault("PK 순서", -1)));
                boolean unique = isTrue(cellText(row, indexes.getOrDefault("UNIQUE", -1)));
                String description = normalizeOptional(cellText(row, indexes.getOrDefault("설명", -1)));

                columns.add(new ColumnDefinition(
                        sequence,
                        columnName.trim(),
                        dataType.trim(),
                        size.trim(),
                        scale.trim(),
                        nullable,
                        defaultValue,
                        pkOrder,
                        unique,
                        description));
            }

            columns.sort(Comparator.comparingInt(ColumnDefinition::sequence));
            return new TableDefinition(tableName, tableDescription.trim(), List.copyOf(columns));
        }

        private static int findHeaderRow(Sheet sheet) {
            int limit = Math.min(sheet.getLastRowNum(), 15);
            for (int rowIndex = 0; rowIndex <= limit; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, Integer> indexes = headerIndexes(row);
                if (indexes.containsKey("컬럼명") && indexes.containsKey("데이터 타입")) {
                    return rowIndex;
                }
            }
            return -1;
        }

        private static Map<String, Integer> headerIndexes(Row row) {
            Map<String, Integer> indexes = new HashMap<>();
            short lastCell = row.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
                String value = cellText(row, cellIndex).trim();
                if (!value.isBlank()) {
                    indexes.put(value.toUpperCase(Locale.ROOT), cellIndex);
                }
            }
            return indexes;
        }

        private static int requiredIndex(Map<String, Integer> indexes, String header) {
            Integer index = indexes.get(header.toUpperCase(Locale.ROOT));
            if (index == null) {
                throw new IllegalArgumentException("필수 헤더가 없습니다: " + header);
            }
            return index;
        }

        private static String cellText(Row row, int cellIndex) {
            if (row == null || cellIndex < 0) {
                return "";
            }
            Cell cell = row.getCell(cellIndex);
            return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
        }

        private static int parseInt(String value, int defaultValue) {
            Integer parsed = parseNullableInt(value);
            return parsed == null ? defaultValue : parsed;
        }

        private static Integer parseNullableInt(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                double number = Double.parseDouble(value.replace(",", ""));
                return (int) number;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static boolean isTrue(String value) {
            String normalized = normalizeOptional(value).toUpperCase(Locale.ROOT);
            return normalized.equals("Y")
                    || normalized.equals("YES")
                    || normalized.equals("TRUE")
                    || normalized.equals("1");
        }

        private static String normalizeOptional(String value) {
            if (value == null) {
                return "";
            }
            String trimmed = value.trim();
            if (isBlankOrPlaceholder(trimmed) || trimmed.equalsIgnoreCase("NULL")) {
                return "";
            }
            return trimmed;
        }

        private static boolean isBlankOrPlaceholder(String value) {
            return value == null || value.isBlank() || value.equals("6") || value.equals("-");
        }
    }

    private static final class SqlGenerator {
        private SqlGenerator() {
        }

        static String generate(List<TableDefinition> tables, DatabaseType databaseType) {
            StringBuilder sql = new StringBuilder();
            for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                TableDefinition table = tables.get(tableIndex);
                String description = table.description().isBlank() ? table.name() : table.description();
                sql.append("=============== ").append(description).append(" ================\n");
                sql.append("CREATE TABLE ").append(table.name()).append(" (\n");

                List<String> definitions = new ArrayList<>();
                for (ColumnDefinition column : table.columns()) {
                    definitions.add(columnSql(column, databaseType));
                }

                List<ColumnDefinition> primaryKeys = table.columns().stream()
                        .filter(column -> column.primaryKeyOrder() != null)
                        .sorted(Comparator.comparingInt(ColumnDefinition::primaryKeyOrder))
                        .toList();
                if (!primaryKeys.isEmpty()) {
                    String keyColumns = primaryKeys.stream()
                            .map(ColumnDefinition::name)
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("");
                    definitions.add("    PRIMARY KEY (" + keyColumns + ")");
                }

                sql.append(String.join(",\n", definitions));
                sql.append("\n);\n");
                if (tableIndex < tables.size() - 1) {
                    sql.append("\n");
                }
            }
            return sql.toString();
        }

        private static String columnSql(ColumnDefinition column, DatabaseType databaseType) {
            StringBuilder definition = new StringBuilder("    ");
            definition.append(column.name())
                    .append(' ')
                    .append(mapType(column, databaseType));

            String defaultValue = mapDefault(column.defaultValue(), databaseType);
            if (!defaultValue.isBlank()) {
                definition.append(" DEFAULT ").append(defaultValue);
            }
            if (!column.nullable()) {
                definition.append(" NOT NULL");
            }
            if (column.unique()) {
                definition.append(" UNIQUE");
            }
            return definition.toString();
        }

        private static String mapType(ColumnDefinition column, DatabaseType databaseType) {
            String baseType = baseType(column.dataType());
            return switch (databaseType) {
                case MYSQL -> mysqlType(baseType, column.size(), column.scale());
                case ORACLE -> oracleType(baseType, column.size(), column.scale());
                case MSSQL -> mssqlType(baseType, column.size(), column.scale());
            };
        }

        private static String mysqlType(String type, String size, String scale) {
            return switch (type) {
                case "VARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2" -> sized("VARCHAR", size, "255");
                case "CHAR", "NCHAR" -> sized("CHAR", size, "1");
                case "INT", "INTEGER" -> "INT";
                case "BIGINT" -> "BIGINT";
                case "SMALLINT" -> "SMALLINT";
                case "TINYINT" -> "TINYINT";
                case "DECIMAL", "NUMERIC", "NUMBER" -> decimalType("DECIMAL", size, scale);
                case "DATETIME" -> "DATETIME";
                case "TIMESTAMP" -> "TIMESTAMP";
                case "DATE" -> "DATE";
                case "BOOLEAN", "BIT" -> "BOOLEAN";
                case "TEXT", "CLOB", "LONGTEXT" -> "TEXT";
                case "BLOB", "BINARY", "VARBINARY" -> "BLOB";
                case "DOUBLE", "BINARY_DOUBLE" -> "DOUBLE";
                case "FLOAT", "REAL", "BINARY_FLOAT" -> "FLOAT";
                default -> originalOrSized(type, size, scale);
            };
        }

        private static String oracleType(String type, String size, String scale) {
            return switch (type) {
                case "VARCHAR", "VARCHAR2" -> sized("VARCHAR2", size, "255");
                case "NVARCHAR", "NVARCHAR2" -> sized("NVARCHAR2", size, "255");
                case "CHAR" -> sized("CHAR", size, "1");
                case "NCHAR" -> sized("NCHAR", size, "1");
                case "INT", "INTEGER", "SMALLINT", "TINYINT" -> "NUMBER(10)";
                case "BIGINT" -> "NUMBER(19)";
                case "DECIMAL", "NUMERIC", "NUMBER" -> decimalType("NUMBER", size, scale);
                case "DATETIME", "TIMESTAMP" -> "TIMESTAMP";
                case "DATE" -> "DATE";
                case "BOOLEAN", "BIT" -> "NUMBER(1)";
                case "TEXT", "CLOB", "LONGTEXT" -> "CLOB";
                case "BLOB", "BINARY", "VARBINARY" -> "BLOB";
                case "DOUBLE", "BINARY_DOUBLE" -> "BINARY_DOUBLE";
                case "FLOAT", "REAL", "BINARY_FLOAT" -> "BINARY_FLOAT";
                default -> originalOrSized(type, size, scale);
            };
        }

        private static String mssqlType(String type, String size, String scale) {
            return switch (type) {
                case "VARCHAR", "VARCHAR2" -> sized("VARCHAR", size, "255");
                case "NVARCHAR", "NVARCHAR2" -> sized("NVARCHAR", size, "255");
                case "CHAR" -> sized("CHAR", size, "1");
                case "NCHAR" -> sized("NCHAR", size, "1");
                case "INT", "INTEGER" -> "INT";
                case "BIGINT" -> "BIGINT";
                case "SMALLINT" -> "SMALLINT";
                case "TINYINT" -> "TINYINT";
                case "DECIMAL", "NUMERIC", "NUMBER" -> decimalType("DECIMAL", size, scale);
                case "DATETIME", "TIMESTAMP" -> "DATETIME2";
                case "DATE" -> "DATE";
                case "BOOLEAN", "BIT" -> "BIT";
                case "TEXT", "CLOB", "LONGTEXT" -> "VARCHAR(MAX)";
                case "BLOB", "BINARY", "VARBINARY" -> "VARBINARY(MAX)";
                case "DOUBLE", "BINARY_DOUBLE", "FLOAT" -> "FLOAT";
                case "REAL", "BINARY_FLOAT" -> "REAL";
                default -> originalOrSized(type, size, scale);
            };
        }

        private static String baseType(String rawType) {
            String normalized = rawType == null ? "" : rawType.trim().toUpperCase(Locale.ROOT);
            int parenthesis = normalized.indexOf('(');
            if (parenthesis >= 0) {
                normalized = normalized.substring(0, parenthesis).trim();
            }
            return normalized;
        }

        private static String sized(String type, String size, String defaultSize) {
            String actualSize = numericPart(size);
            if (actualSize.isBlank()) {
                actualSize = defaultSize;
            }
            return type + "(" + actualSize + ")";
        }

        private static String decimalType(String type, String size, String scale) {
            String precision = numericPart(size);
            String decimalScale = numericPart(scale);
            if (precision.isBlank()) {
                precision = "38";
            }
            if (decimalScale.isBlank()) {
                return type + "(" + precision + ")";
            }
            return type + "(" + precision + "," + decimalScale + ")";
        }

        private static String originalOrSized(String type, String size, String scale) {
            if (!numericPart(scale).isBlank()) {
                return decimalType(type, size, scale);
            }
            String length = numericPart(size);
            return length.isBlank() ? type : type + "(" + length + ")";
        }

        private static String numericPart(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String normalized = value.trim();
            try {
                double number = Double.parseDouble(normalized.replace(",", ""));
                long whole = (long) number;
                return String.valueOf(whole);
            } catch (NumberFormatException ignored) {
                return normalized.replaceAll("[^0-9]", "");
            }
        }

        private static String mapDefault(String value, DatabaseType databaseType) {
            if (value == null || value.isBlank()) {
                return "";
            }

            String trimmed = value.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT).replace(" ", "");
            if (lower.equals("current_timestamp()") || lower.equals("current_timestamp") || lower.equals("now()")) {
                return "CURRENT_TIMESTAMP";
            }
            if (trimmed.equalsIgnoreCase("true")) {
                return databaseType == DatabaseType.MYSQL ? "TRUE" : "1";
            }
            if (trimmed.equalsIgnoreCase("false")) {
                return databaseType == DatabaseType.MYSQL ? "FALSE" : "0";
            }
            return trimmed;
        }
    }
}
