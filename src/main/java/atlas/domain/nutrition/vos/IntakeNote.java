package atlas.domain.nutrition.vos;

import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import java.util.Optional;

public record IntakeNote(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 140;

    public IntakeNote {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<Optional<IntakeNote>> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.success(Optional.empty());
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(IntakeErrors.NOTE_TOO_LONG);
        }

        return Result.success(Optional.of(new IntakeNote(trimmed)));
    }
}
