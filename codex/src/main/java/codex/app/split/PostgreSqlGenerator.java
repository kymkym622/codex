package codex.app.split;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import codex.app.split.SchemaModel.ColumnDefinition;
import codex.app.split.SchemaModel.TableDefinition;

public final class PostgreSqlGenerator implements SqlGenerator {
    @Override
    public String generate(List<TableDefinition> tables) {
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            TableDefinition table = tables.get(i);
            String tableName = lower(table.name());
            String separator = table.description().isBlank() ? tableName : table.description();
            sql.append("=============== ").append(separator).append(" ================\n");
            sql.append("create table ").append(tableName).append(" (\n");

            List<String> definitions = new ArrayList<>();
            for (ColumnDefinition column : table.columns()) definitions.add(columnSql(column));
            List<ColumnDefinition> pks = table.columns().stream()
                    .filter(c -> c.primaryKeyOrder() != null)
                    .sorted(Comparator.comparingInt(ColumnDefinition::primaryKeyOrder)).toList();
            if (!pks.isEmpty()) {
                definitions.add("    primary key (" + String.join(", ", pks.stream().map(c -> lower(c.name())).toList()) + ")");
            }

            sql.append(String.join(",\n", definitions)).append("\n);\n\n");
            sql.append("comment on table ").append(tableName).append(" is '")
                    .append(escape(table.description())).append("';\n");
            for (ColumnDefinition column : table.columns()) {
                sql.append("comment on column ").append(tableName).append('.').append(lower(column.name()))
                        .append(" is '").append(escape(column.description())).append("';\n");
            }
            if (i < tables.size() - 1) sql.append("\n\n");
        }
        return sql.toString();
    }

    private static String columnSql(ColumnDefinition column) {
        StringBuilder sql = new StringBuilder("    ");
        sql.append(lower(column.name())).append(' ').append(type(column));
        if (!column.nullable()) sql.append(" not null");
        if (!column.defaultValue().isBlank()) sql.append(" default ").append(column.defaultValue());
        if (column.unique()) sql.append(" unique");
        return sql.toString();
    }

    private static String type(ColumnDefinition column) {
        String type = column.dataType().trim().toUpperCase(Locale.ROOT);
        String size = clean(column.size());
        if (isNumeric(type)) return "integer";
        if (type.equals("DATE") || type.equals("DATETIME") || type.contains("TIMESTAMP")) return "timestamp";
        if (type.equals("VARCHAR") || type.equals("VARCHAR2") || type.equals("STRING") || type.equals("NVARCHAR") || type.equals("NVARCHAR2"))
            return "varchar(" + (size.isBlank() ? "255" : size) + ")";
        if (type.equals("CHAR") || type.equals("NCHAR")) return "char(" + (size.isBlank() ? "1" : size) + ")";
        if (type.equals("CLOB") || type.equals("TEXT")) return "text";
        if (type.equals("BLOB") || type.equals("BINARY")) return "bytea";
        return appendOriginal(type.toLowerCase(Locale.ROOT), size, clean(column.scale()));
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

    private static String lower(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static String escape(String value) { return value == null ? "" : value.replace("'", "''"); }
}
