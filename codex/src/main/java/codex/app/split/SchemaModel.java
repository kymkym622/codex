package codex.app.split;

import java.util.List;

public final class SchemaModel {
    private SchemaModel() {}

    public record ColumnDefinition(
            int sequence,
            String name,
            String dataType,
            String size,
            String scale,
            boolean nullable,
            String defaultValue,
            Integer primaryKeyOrder,
            boolean unique,
            String description) {}

    public record TableDefinition(
            String name,
            String description,
            List<ColumnDefinition> columns) {}
}
