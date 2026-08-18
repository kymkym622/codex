package codex.app.split;

import java.util.List;
import codex.app.split.SchemaModel.TableDefinition;

public interface SqlGenerator {
    String generate(List<TableDefinition> tables);
}
