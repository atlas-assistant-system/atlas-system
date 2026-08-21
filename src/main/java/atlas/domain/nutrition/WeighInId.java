package atlas.domain.nutrition;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record WeighInId(long value) {

    private static final char PREFIX = 'W';
    private static final int NUMERIC_LENGTH = 8;

    public WeighInId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static WeighInId of(long value) {
        return new WeighInId(value);
    }

    public static WeighInId parse(String text) {
        return new WeighInId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<WeighInId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(WeighInId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
