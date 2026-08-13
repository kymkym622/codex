package codex.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTextField;

public final class WelcomeAppTest {
    private WelcomeAppTest() {
    }

    public static void main(String[] args) {
        Integer[] options = WelcomeApp.createCountOptions();
        check(options.length == 19, "선택 항목은 2~20까지 19개여야 합니다.");
        check(options[0] == 2, "첫 선택 항목은 2여야 합니다.");
        check(options[options.length - 1] == 20, "마지막 선택 항목은 20이어야 합니다.");

        JTextField emptyField = new JTextField("");
        check(WelcomeApp.selectRandomValue(List.of(emptyField)).isEmpty(),
                "빈 입력칸을 뽑으면 빈값이어야 합니다.");

        List<JTextField> fields = Arrays.asList(
                new JTextField("사과"),
                new JTextField("바나나"),
                new JTextField("포도"));
        Set<String> allowedValues = new HashSet<>(Arrays.asList("사과", "바나나", "포도"));
        for (int attempt = 0; attempt < 100; attempt++) {
            check(allowedValues.contains(WelcomeApp.selectRandomValue(fields)),
                    "결과는 입력칸 중 하나여야 합니다.");
        }

        System.out.println("모든 테스트 통과");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
