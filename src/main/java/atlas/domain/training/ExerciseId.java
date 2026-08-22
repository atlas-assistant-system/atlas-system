package atlas.domain.training;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record ExerciseId(long value) {

    private static final char PREFIX = 'E';
    private static final int NUMERIC_LENGTH = 8;

    public ExerciseId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static ExerciseId of(long value) {
        return new ExerciseId(value);
    }

    public static ExerciseId parse(String text) {
        return new ExerciseId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<ExerciseId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(ExerciseId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
