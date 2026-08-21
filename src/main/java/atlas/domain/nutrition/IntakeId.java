package atlas.domain.nutrition;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record IntakeId(long value) {

    private static final char PREFIX = 'I';
    private static final int NUMERIC_LENGTH = 8;

    public IntakeId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static IntakeId of(long value) {
        return new IntakeId(value);
    }

    public static IntakeId parse(String text) {
        return new IntakeId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<IntakeId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(IntakeId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
