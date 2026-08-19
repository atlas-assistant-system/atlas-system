package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.guards.StringGuard;
import sharedkernel.domain.results.Result;

public record RoutineName(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 80;

    public RoutineName {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<RoutineName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(RoutineErrors.NAME_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(RoutineErrors.NAME_TOO_LONG);
        }

        return Result.success(new RoutineName(trimmed));
    }

    @Override
    public String toString() {
        return value;
    }
}
