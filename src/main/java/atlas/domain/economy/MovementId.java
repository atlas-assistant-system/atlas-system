package atlas.domain.economy;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record MovementId(long value) {

    private static final char PREFIX = 'M';
    private static final int NUMERIC_LENGTH = 8;

    public MovementId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static MovementId of(long value) {
        return new MovementId(value);
    }

    public static MovementId parse(String text) {
        return new MovementId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<MovementId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(MovementId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
