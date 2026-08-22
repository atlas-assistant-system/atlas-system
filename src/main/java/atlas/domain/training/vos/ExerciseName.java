package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseErrors;

public record ExerciseName(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 60;

    public ExerciseName {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<ExerciseName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ExerciseErrors.NAME_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(ExerciseErrors.NAME_TOO_LONG);
        }

        return Result.success(new ExerciseName(trimmed));
    }

    @Override
    public String toString() {
        return value;
    }
}
