package codex.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private final String source;
    private int index;

    private SimpleJson(String source) {
        this.source = source;
    }

    static Object parse(String source) {
        SimpleJson parser = new SimpleJson(source);
        Object result = parser.readValue();
        parser.skipWhitespace();
        if (parser.index != source.length()) {
            throw parser.error("JSON 뒤에 불필요한 문자가 있습니다.");
        }
        return result;
    }

    private Object readValue() {
        skipWhitespace();
        if (index >= source.length()) {
            throw error("JSON 값이 필요합니다.");
        }
        return switch (source.charAt(index)) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            result.put(key, readValue());
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            expect(',');
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return result;
        }
        while (true) {
            result.add(readValue());
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            expect(',');
        }
    }

    private String readString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (index < source.length()) {
            char current = source.charAt(index++);
            if (current == '"') {
                return result.toString();
            }
            if (current != '\\') {
                result.append(current);
                continue;
            }
            if (index >= source.length()) {
                throw error("문자열 이스케이프가 끝나지 않았습니다.");
            }
            char escaped = source.charAt(index++);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> result.append(readUnicode());
                default -> throw error("지원하지 않는 이스케이프입니다.");
            }
        }
        throw error("문자열이 끝나지 않았습니다.");
    }

    private char readUnicode() {
        if (index + 4 > source.length()) {
            throw error("유니코드 이스케이프가 잘못되었습니다.");
        }
        String hex = source.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            throw error("유니코드 이스케이프가 잘못되었습니다.");
        }
    }

    private Object readNumber() {
        int start = index;
        if (consume('-')) {
            // 부호를 소비한다.
        }
        readDigits();
        boolean decimal = false;
        if (consume('.')) {
            decimal = true;
            readDigits();
        }
        if (consume('e') || consume('E')) {
            decimal = true;
            if (consume('+') || consume('-')) {
                // 지수 부호를 소비한다.
            }
            readDigits();
        }
        if (start == index) {
            throw error("숫자 형식이 잘못되었습니다.");
        }
        String number = source.substring(start, index);
        try {
            if (decimal) {
                return Double.valueOf(number);
            }
            return Long.valueOf(number);
        } catch (NumberFormatException exception) {
            throw error("숫자 형식이 잘못되었습니다.");
        }
    }

    private void readDigits() {
        int start = index;
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
            index++;
        }
        if (start == index) {
            throw error("숫자가 필요합니다.");
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!source.startsWith(literal, index)) {
            throw error("잘못된 JSON 값입니다.");
        }
        index += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
    }

    private boolean consume(char expected) {
        if (index < source.length() && source.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw error("'" + expected + "' 문자가 필요합니다.");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " 위치: " + index);
    }
}
