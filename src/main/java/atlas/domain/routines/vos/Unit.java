package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import java.util.Optional;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.guards.StringGuard;
import sharedkernel.domain.results.Result;

public record Unit(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 16;

    public Unit {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<Optional<Unit>> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.success(Optional.empty());
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(RoutineErrors.UNIT_TOO_LONG);
        }

        return Result.success(Optional.of(new Unit(trimmed)));
    }

    @Override
    public String toString() {
        return value;
    }
}
