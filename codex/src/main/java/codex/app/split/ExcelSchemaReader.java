package codex.app.split;

import java.io.File;
import java.io.IOException;
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

import codex.app.split.SchemaModel.ColumnDefinition;
import codex.app.split.SchemaModel.TableDefinition;

public final class ExcelSchemaReader {
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.KOREA);

    private ExcelSchemaReader() {}

    public static List<TableDefinition> read(File file) throws IOException {
        List<TableDefinition> tables = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Map<String, String> tableDescriptions = readTableDescriptions(workbook);

            for (Sheet sheet : workbook) {
                if (sheet.getSheetName().equalsIgnoreCase("목록") || sheet.getSheetName().equalsIgnoreCase("index")) {
                    continue;
                }

                String tableName = sheet.getSheetName().trim();
                String tableDescription = tableDescriptions.getOrDefault(tableName.toLowerCase(Locale.ROOT), "");
                TableDefinition table = parseSheet(sheet, tableDescription);
                if (table != null && !table.columns().isEmpty()) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    private static Map<String, String> readTableDescriptions(Workbook workbook) {
        Map<String, String> descriptions = new HashMap<>();
        Sheet indexSheet = workbook.getSheet("목록");
        if (indexSheet == null) {
            indexSheet = workbook.getSheet("index");
        }
        if (indexSheet == null) {
            return descriptions;
        }

        int headerRowIndex = findIndexHeaderRow(indexSheet);
        if (headerRowIndex < 0) {
            return descriptions;
        }

        Map<String, Integer> indexes = headerIndexes(indexSheet.getRow(headerRowIndex));
        Integer tableIndex = indexes.get("테이블/뷰");
        Integer descriptionIndex = indexes.get("설명");
        if (tableIndex == null || descriptionIndex == null) {
            return descriptions;
        }

        for (int rowIndex = headerRowIndex + 1; rowIndex <= indexSheet.getLastRowNum(); rowIndex++) {
            Row row = indexSheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String tableName = cellText(row, tableIndex);
            if (tableName.isBlank()) {
                continue;
            }

            String description = normalizeOptional(cellText(row, descriptionIndex));
            descriptions.put(tableName.trim().toLowerCase(Locale.ROOT), description);
        }

        return descriptions;
    }

    private static int findIndexHeaderRow(Sheet sheet) {
        int limit = Math.min(sheet.getLastRowNum(), 20);
        for (int rowIndex = 0; rowIndex <= limit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Map<String, Integer> indexes = headerIndexes(row);
            if (indexes.containsKey("테이블/뷰") && indexes.containsKey("설명")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private static TableDefinition parseSheet(Sheet sheet, String tableDescription) {
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0) return null;
        Map<String, Integer> indexes = headerIndexes(sheet.getRow(headerRowIndex));
        int nameIndex = requiredIndex(indexes, "컬럼명");
        int typeIndex = requiredIndex(indexes, "데이터 타입");

        String tableName = sheet.getSheetName().trim();
        List<ColumnDefinition> columns = new ArrayList<>();

        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String columnName = cellText(row, nameIndex);
            String dataType = cellText(row, typeIndex);
            if (columnName.isBlank() || dataType.isBlank()) continue;

            int sequence = parseInt(cellText(row, indexes.getOrDefault("순번", -1)), columns.size() + 1);
            String size = cellText(row, indexes.getOrDefault("크기", -1));
            String scale = cellText(row, indexes.getOrDefault("소수점", -1));
            String nullableText = cellText(row, indexes.getOrDefault("NULL 허용", -1));
            boolean nullable = !nullableText.equalsIgnoreCase("N") && !nullableText.equalsIgnoreCase("NO");
            String defaultValue = normalizeOptional(cellText(row, indexes.getOrDefault("기본값", -1)));
            Integer pkOrder = parseNullableInt(cellText(row, indexes.getOrDefault("PK 순서", -1)));
            boolean unique = isTrue(cellText(row, indexes.getOrDefault("UNIQUE", -1)));
            String description = normalizeOptional(cellText(row, indexes.getOrDefault("설명", -1)));

            columns.add(new ColumnDefinition(sequence, columnName.trim(), dataType.trim(), size.trim(), scale.trim(),
                    nullable, defaultValue, pkOrder, unique, description));
        }
        columns.sort(Comparator.comparingInt(ColumnDefinition::sequence));
        return new TableDefinition(tableName, tableDescription, List.copyOf(columns));
    }

    private static int findHeaderRow(Sheet sheet) {
        int limit = Math.min(sheet.getLastRowNum(), 15);
        for (int rowIndex = 0; rowIndex <= limit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            Map<String, Integer> indexes = headerIndexes(row);
            if (indexes.containsKey("컬럼명") && indexes.containsKey("데이터 타입")) return rowIndex;
        }
        return -1;
    }

    private static Map<String, Integer> headerIndexes(Row row) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String value = cellText(row, i).trim();
            if (!value.isBlank()) indexes.put(value.toUpperCase(Locale.ROOT), i);
        }
        return indexes;
    }

    private static int requiredIndex(Map<String, Integer> indexes, String header) {
        Integer index = indexes.get(header.toUpperCase(Locale.ROOT));
        if (index == null) throw new IllegalArgumentException("필수 헤더가 없습니다: " + header);
        return index;
    }

    private static String cellText(Row row, int cellIndex) {
        if (row == null || cellIndex < 0) return "";
        Cell cell = row.getCell(cellIndex);
        return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
    }

    private static int parseInt(String value, int defaultValue) {
        Integer parsed = parseNullableInt(value);
        return parsed == null ? defaultValue : parsed;
    }

    private static Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) return null;
        try { return (int) Double.parseDouble(value.replace(",", "")); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static boolean isTrue(String value) {
        String normalized = normalizeOptional(value).toUpperCase(Locale.ROOT);
        return normalized.equals("Y") || normalized.equals("YES") || normalized.equals("TRUE") || normalized.equals("1");
    }

    private static String normalizeOptional(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.equals("6") || trimmed.equals("-") || trimmed.equalsIgnoreCase("NULL")) return "";
        return trimmed;
    }
}
