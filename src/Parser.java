import java.util.List;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private ValidationResult result;

    public SelectStatement parse(List<Token> tokens, ValidationResult result) {
        this.tokens = tokens;
        this.pos = 0;
        this.result = result;
        SelectStatement statement = new SelectStatement();
        expect(TokenType.SELECT, "SYNTACTIC_EXPECTED_SELECT");
        parseColumns(statement);
        expect(TokenType.FROM, "SYNTACTIC_EXPECTED_FROM");
        Token table = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_TABLE");
        if (table != null) statement.table = table.lexeme;

        // Parseo de WHERE (opcional)
        if (match(TokenType.WHERE)) {
            ConditionChain cadena = new ConditionChain();
            while (true) {
                // Leer nombre de columna
                Token columna = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_WHERE_COLUMN");
                if (columna == null) {
                    while (!check(TokenType.EOF) && !check(TokenType.SEMICOLON)) advance();
                    break;
                }
                SourceSpan spanColumna = columna.span;
                String nombreColumna = columna.lexeme;

                // Leer operador de comparacion
                Token operador = current();
                boolean esOperador = operador.type == TokenType.EQUAL ||
                    operador.type == TokenType.GREATER ||
                    operador.type == TokenType.LESS ||
                    operador.type == TokenType.GREATER_EQUAL ||
                    operador.type == TokenType.LESS_EQUAL ||
                    operador.type == TokenType.NOT_EQUAL;

                if (!esOperador) {
                    result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERATOR",
                        "Se esperaba un operador de comparacion", current().span));
                    while (!check(TokenType.EOF) && !check(TokenType.SEMICOLON)) advance();
                    break;
                }
                Token tokOperador = advance();
                String operadorStr = tokOperador.lexeme;
                SourceSpan spanOperador = tokOperador.span;

                // Leer literal (numero, string o booleano)
                Token literal = current();
                LiteralType tipoLiteral = null;
                if (literal.type == TokenType.NUMBER) {
                    tipoLiteral = LiteralType.NUMBER;
                } else if (literal.type == TokenType.STRING) {
                    tipoLiteral = LiteralType.STRING;
                } else if (literal.type == TokenType.TRUE || literal.type == TokenType.FALSE) {
                    tipoLiteral = LiteralType.BOOLEAN;
                }

                if (tipoLiteral == null) {
                    result.diagnostics.add(new Diagnostic("SYNTACTIC_EXPECTED_WHERE_OPERAND",
                        "Se esperaba un literal despues del operador", current().span));
                    break;
                }
                Token tokLiteral = advance();
                SourceSpan spanLiteral = tokLiteral.span;

                cadena.conditions.add(new WhereCondition(nombreColumna, operadorStr,
                    tokLiteral.lexeme, tipoLiteral, spanColumna, spanOperador, spanLiteral));

                // Verificar si hay AND u OR para seguir leyendo condiciones
                if (match(TokenType.AND)) {
                    cadena.connectors.add("AND");
                    continue;
                }
                if (match(TokenType.OR)) {
                    cadena.connectors.add("OR");
                    continue;
                }
                break;
            }
            if (cadena.conditions.size() > 0) {
                statement.where = cadena;
            }
        }

        if (check(TokenType.SEMICOLON)) advance();
        if (!check(TokenType.EOF)) {
            result.diagnostics.add(new Diagnostic("SYNTACTIC_UNEXPECTED_TOKEN", "Token inesperado: " + current().lexeme, current().span));
        }
        return statement;
    }

    private void parseColumns(SelectStatement statement) {
        if (match(TokenType.STAR)) {
            statement.columns.add("*");
            return;
        }
        Token first = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
        if (first != null) statement.columns.add(first.lexeme);
        while (match(TokenType.COMMA)) {
            Token next = expect(TokenType.IDENTIFIER, "SYNTACTIC_EXPECTED_COLUMN");
            if (next != null) statement.columns.add(next.lexeme);
        }
    }

    private Token expect(TokenType type, String code) {
        if (check(type)) return advance();
        result.diagnostics.add(new Diagnostic(code, "Se esperaba " + type + " y se encontro " + current().type, current().span));
        return null;
    }

    private boolean match(TokenType type) { if (check(type)) { advance(); return true; } return false; }
    private boolean check(TokenType type) { return current().type == type; }
    private Token current() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }
}
