package atlas.domain.economy.vos;

import atlas.domain.economy.SavingsGoalErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;

public record GoalName(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 60;

    public GoalName {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<GoalName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(SavingsGoalErrors.NAME_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(SavingsGoalErrors.NAME_TOO_LONG);
        }

        return Result.success(new GoalName(trimmed));
    }
}
