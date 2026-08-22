package atlas.domain.training;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record WorkoutId(long value) {

    private static final char PREFIX = 'W';
    private static final int NUMERIC_LENGTH = 8;

    public WorkoutId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static WorkoutId of(long value) {
        return new WorkoutId(value);
    }

    public static WorkoutId parse(String text) {
        return new WorkoutId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<WorkoutId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(WorkoutId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
