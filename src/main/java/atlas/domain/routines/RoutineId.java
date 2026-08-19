package atlas.domain.routines;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record RoutineId(long value) {

    private static final char PREFIX = 'R';
    private static final int NUMERIC_LENGTH = 8;

    public RoutineId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static RoutineId of(long value) {
        return new RoutineId(value);
    }

    public static RoutineId parse(String text) {
        return new RoutineId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<RoutineId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(RoutineId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
