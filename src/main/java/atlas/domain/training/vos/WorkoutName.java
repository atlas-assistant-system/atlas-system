package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;

public record WorkoutName(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 60;

    public WorkoutName {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<WorkoutName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(WorkoutErrors.NAME_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(WorkoutErrors.NAME_TOO_LONG);
        }

        return Result.success(new WorkoutName(trimmed));
    }

    @Override
    public String toString() {
        return value;
    }
}
