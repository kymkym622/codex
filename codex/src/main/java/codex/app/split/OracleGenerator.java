package codex.app.split;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import codex.app.split.SchemaModel.ColumnDefinition;
import codex.app.split.SchemaModel.TableDefinition;

public final class OracleGenerator implements SqlGenerator {
    @Override
    public String generate(List<TableDefinition> tables) {
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            TableDefinition table = tables.get(i);
            String tableName = upper(table.name());
            String separator = table.description().isBlank() ? tableName : table.description();
            sql.append("=============== ").append(separator).append(" ================\n");
            sql.append("CREATE TABLE ").append(tableName).append(" (\n");

            List<String> definitions = new ArrayList<>();
            for (ColumnDefinition column : table.columns()) definitions.add(columnSql(column));
            List<ColumnDefinition> pks = table.columns().stream()
                    .filter(c -> c.primaryKeyOrder() != null)
                    .sorted(Comparator.comparingInt(ColumnDefinition::primaryKeyOrder)).toList();
            if (!pks.isEmpty()) {
                definitions.add("    PRIMARY KEY (" + String.join(", ", pks.stream().map(c -> upper(c.name())).toList()) + ")");
            }

            sql.append(String.join(",\n", definitions)).append("\n);\n\n");
            sql.append("COMMENT ON TABLE ").append(tableName).append(" IS '")
                    .append(escape(table.description())).append("';\n");
            for (ColumnDefinition column : table.columns()) {
                sql.append("COMMENT ON COLUMN ").append(tableName).append('.').append(upper(column.name()))
                        .append(" IS '").append(escape(column.description())).append("';\n");
            }
            if (i < tables.size() - 1) sql.append("\n\n");
        }
        return sql.toString();
    }

    private static String columnSql(ColumnDefinition column) {
        StringBuilder sql = new StringBuilder("    ");
        sql.append(upper(column.name())).append(' ').append(type(column));
        if (!column.nullable()) sql.append(" NOT NULL");
        if (!column.defaultValue().isBlank()) sql.append(" DEFAULT ").append(column.defaultValue());
        if (column.unique()) sql.append(" UNIQUE");
        return sql.toString();
    }

    private static String type(ColumnDefinition column) {
        String type = column.dataType().trim().toUpperCase(Locale.ROOT);
        String size = clean(column.size());
        if (isNumeric(type)) return "NUMBER";
        if (type.equals("DATE")) return "DATE";
        if (type.equals("DATETIME") || type.contains("TIMESTAMP")) return "TIMESTAMP";
        if (type.equals("VARCHAR") || type.equals("VARCHAR2") || type.equals("STRING")) return "VARCHAR2(" + (size.isBlank() ? "255" : size) + ")";
        if (type.equals("NVARCHAR") || type.equals("NVARCHAR2")) return "NVARCHAR2(" + (size.isBlank() ? "255" : size) + ")";
        if (type.equals("CHAR") || type.equals("NCHAR")) return type + "(" + (size.isBlank() ? "1" : size) + ")";
        if (type.equals("TEXT")) return "CLOB";
        if (type.equals("BINARY")) return "BLOB";
        return appendOriginal(type, size, clean(column.scale()));
    }

    private static boolean isNumeric(String type) {
        return type.equals("NUMBER") || type.equals("NUMERIC") || type.equals("DECIMAL") || type.equals("INTEGER")
                || type.equals("INT") || type.equals("BIGINT") || type.equals("LONG");
    }

    private static String appendOriginal(String type, String size, String scale) {
        if (size.isBlank()) return type;
        return scale.isBlank() ? type + "(" + size + ")" : type + "(" + size + "," + scale + ")";
    }

    private static String clean(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.endsWith(".0") ? v.substring(0, v.length() - 2) : v;
    }

    private static String upper(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String escape(String value) { return value == null ? "" : value.replace("'", "''"); }
}
