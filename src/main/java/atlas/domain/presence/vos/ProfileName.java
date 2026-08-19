package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.guards.StringGuard;
import sharedkernel.domain.results.Result;

public record ProfileName(String value) implements SingleValueObject<String> {

    public static final int MAX_LENGTH = 60;

    public ProfileName {
        StringGuard.notBlank(value, "value");
        StringGuard.notLongerThan(value, MAX_LENGTH, "value");
    }

    public static Result<ProfileName> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(PresenceErrors.PROFILE_NAME_REQUIRED);
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(PresenceErrors.PROFILE_NAME_TOO_LONG);
        }

        return Result.success(new ProfileName(trimmed));
    }

    public static ProfileName of(String value) {
        return new ProfileName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
