package atlas.domain.economy;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record BudgetId(long value) {

    private static final char PREFIX = 'P';
    private static final int NUMERIC_LENGTH = 8;

    public BudgetId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static BudgetId of(long value) {
        return new BudgetId(value);
    }

    public static BudgetId parse(String text) {
        return new BudgetId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<BudgetId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(BudgetId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
