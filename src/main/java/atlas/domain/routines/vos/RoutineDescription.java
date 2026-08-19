package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import java.util.Optional;

public record RoutineDescription(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 1000;

    public RoutineDescription {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<Optional<RoutineDescription>> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.success(Optional.empty());
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(RoutineErrors.DESCRIPTION_TOO_LONG);
        }

        return Result.success(Optional.of(new RoutineDescription(trimmed)));
    }

    @Override
    public String toString() {
        return value;
    }
}
