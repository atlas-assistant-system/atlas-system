package atlas.domain.nutrition;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record PlanId(long value) {

    private static final char PREFIX = 'N';
    private static final int NUMERIC_LENGTH = 8;

    public PlanId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static PlanId of(long value) {
        return new PlanId(value);
    }

    public static PlanId parse(String text) {
        return new PlanId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<PlanId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(PlanId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
