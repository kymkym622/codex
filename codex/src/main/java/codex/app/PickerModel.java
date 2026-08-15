package codex.app;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public final class PickerModel {
    public static final int MIN_COUNT = 2;
    public static final int MAX_COUNT = 20;

    private PickerModel() {
    }

    public static List<Integer> countOptions() {
        return IntStream.rangeClosed(MIN_COUNT, MAX_COUNT).boxed().toList();
    }

    public static String selectRandomValue(List<String> values, RandomGenerator random) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(random, "random");

        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }

        return values.get(random.nextInt(values.size()));
    }
}
