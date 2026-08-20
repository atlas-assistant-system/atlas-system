package atlas.domain.economy;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record SavingsGoalId(long value) {

    private static final char PREFIX = 'O';
    private static final int NUMERIC_LENGTH = 8;

    public SavingsGoalId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static SavingsGoalId of(long value) {
        return new SavingsGoalId(value);
    }

    public static SavingsGoalId parse(String text) {
        return new SavingsGoalId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<SavingsGoalId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(SavingsGoalId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
