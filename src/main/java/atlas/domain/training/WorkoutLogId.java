package atlas.domain.training;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record WorkoutLogId(long value) {

    private static final char PREFIX = 'T';
    private static final int NUMERIC_LENGTH = 8;

    public WorkoutLogId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static WorkoutLogId of(long value) {
        return new WorkoutLogId(value);
    }

    public static WorkoutLogId parse(String text) {
        return new WorkoutLogId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<WorkoutLogId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(WorkoutLogId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
