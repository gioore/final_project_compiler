import java.util.HashMap;
import java.util.Map;

public class SemanticValidator {
    private final Map<String, Map<String, LiteralType>> schema = new HashMap<String, Map<String, LiteralType>>();

    public SemanticValidator() {
        addTable("usuarios", new String[][]{{"nombre", "STRING"}, {"edad", "NUMBER"}, {"activo", "BOOLEAN"}});
        addTable("productos", new String[][]{{"nombre", "STRING"}, {"precio", "NUMBER"}});
    }

    private void addTable(String table, String[][] columns) {
        Map<String, LiteralType> cols = new HashMap<String, LiteralType>();
        for (int i = 0; i < columns.length; i++) cols.put(columns[i][0], LiteralType.valueOf(columns[i][1]));
        schema.put(table, cols);
    }

    public void validate(SelectStatement ast, ValidationResult result) {
        if (ast == null || ast.table == null) return;
        Map<String, LiteralType> table = schema.get(ast.table.toLowerCase());
        if (table == null) {
            result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_TABLE", "Tabla no existe: " + ast.table, new SourceSpan(1, 1)));
            return;
        }
        for (int i = 0; i < ast.columns.size(); i++) {
            String col = ast.columns.get(i);
            if (!col.equals("*") && !table.containsKey(col.toLowerCase())) {
                result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_COLUMN", "Columna no existe: " + col, new SourceSpan(1, 1)));
            }
        }
        // TODO SERIE 2: validar WHERE
        // - SEMANTIC_UNKNOWN_WHERE_COLUMN
        // - SEMANTIC_TYPE_MISMATCH
        // - TRACE|WHERE_TYPE_CHECK|<line>:<column>|<column>|<operator>|<literalType>
        if (ast.where != null) {
            for (int i = 0; i < ast.where.conditions.size(); i++) {
                WhereCondition cond = ast.where.conditions.get(i);
                String nombreColumna = cond.column.toLowerCase();
                LiteralType tipoColumna = table.get(nombreColumna);
                if (tipoColumna == null) {
                    result.diagnostics.add(new Diagnostic("SEMANTIC_UNKNOWN_WHERE_COLUMN",
                        "Columna no existe en WHERE: " + cond.column, cond.columnSpan));
                } else if (tipoColumna != cond.literalType) {
                    result.diagnostics.add(new Diagnostic("SEMANTIC_TYPE_MISMATCH",
                        "Tipo de dato no coincide para columna " + cond.column +
                        ": se esperaba " + tipoColumna + " pero se encontro " + cond.literalType,
                        cond.columnSpan));
                }
                // Siempre agregar la traza, incluso si hay error
                result.traces.add("TRACE|WHERE_TYPE_CHECK|" +
                    cond.columnSpan.format() + "|" +
                    cond.column + "|" + cond.operator + "|" + cond.literalType);
            }
        }
    }
}
