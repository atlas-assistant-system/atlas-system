package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;

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
