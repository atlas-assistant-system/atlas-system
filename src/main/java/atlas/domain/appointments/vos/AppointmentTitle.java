package atlas.domain.appointments.vos;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;

public record AppointmentTitle(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 120;

    public AppointmentTitle {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<AppointmentTitle> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(AppointmentErrors.TITLE_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(AppointmentErrors.TITLE_TOO_LONG);
        }

        return Result.success(new AppointmentTitle(trimmed));
    }

    @Override
    public String toString() {
        return value;
    }
}
