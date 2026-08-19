package atlas.domain.sharedkernel.guards;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.time.Instant;

public final class TimeGuard {

    private TimeGuard() {}

    public static Instant notInFuture(Instant value, Instant now, String parameterName) {
        ObjectGuard.notNull(value, parameterName);
        ObjectGuard.notNull(now, "now");

        if (value.isAfter(now)) {
            throw GuardException.forParameter(parameterName, "cannot be in the future");
        }

        return value;
    }

    public static Instant notInPast(Instant value, Instant now, String parameterName) {
        ObjectGuard.notNull(value, parameterName);
        ObjectGuard.notNull(now, "now");

        if (value.isBefore(now)) {
            throw GuardException.forParameter(parameterName, "cannot be in the past");
        }

        return value;
    }
}
