package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Duration;

public record SessionDuration(Duration value) implements SingleValueObject<Duration> {

    public static final Duration MAX_DURATION = Duration.ofHours(24);

    public SessionDuration {
        ObjectGuard.notNull(value, "value");

        if (value.isZero() || value.isNegative()) {
            throw GuardException.forParameter("value", "must be positive");
        }

        if (value.compareTo(MAX_DURATION) > 0) {
            throw GuardException.forParameter("value", "cannot exceed 24 hours");
        }
    }

    public static Result<SessionDuration> create(Duration value) {
        if (value == null || value.isZero() || value.isNegative() || value.compareTo(MAX_DURATION) > 0) {
            return Result.failure(PresenceErrors.INVALID_SESSION_DURATION);
        }

        return Result.success(new SessionDuration(value));
    }

    public static SessionDuration of(Duration value) {
        return new SessionDuration(value);
    }
}
