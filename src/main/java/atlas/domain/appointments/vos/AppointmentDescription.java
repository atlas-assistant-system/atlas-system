package atlas.domain.appointments.vos;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import java.util.Optional;

public record AppointmentDescription(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 2000;

    public AppointmentDescription {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<Optional<AppointmentDescription>> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.success(Optional.empty());
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(AppointmentErrors.DESCRIPTION_TOO_LONG);
        }

        return Result.success(Optional.of(new AppointmentDescription(trimmed)));
    }

    @Override
    public String toString() {
        return value;
    }
}
