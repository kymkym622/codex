package codex.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class PickerModelTest {
    @Test
    void providesEveryCountFromTwoThroughTwenty() {
        List<Integer> options = PickerModel.countOptions();

        assertEquals(19, options.size());
        assertEquals(2, options.get(0));
        assertEquals(20, options.get(options.size() - 1));
    }

    @Test
    void returnsEmptyTextWhenTheOnlyFieldIsEmpty() {
        assertEquals("", PickerModel.selectRandomValue(List.of(""), new Random(1)));
    }

    @Test
    void returnsOneOfTheProvidedValues() {
        List<String> values = List.of("사과", "바나나", "포도");

        for (int attempt = 0; attempt < 100; attempt++) {
            assertTrue(values.contains(
                    PickerModel.selectRandomValue(values, new Random(attempt))));
        }
    }

    @Test
    void rejectsAnEmptyFieldList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PickerModel.selectRandomValue(List.of(), new Random(1)));
    }
}
