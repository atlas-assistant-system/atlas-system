package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.exceptions.GuardException;
import sharedkernel.domain.guards.ObjectGuard;
import sharedkernel.domain.results.Result;

public record MatchThreshold(Double value) implements SingleValueObject<Double> {

    public static final double MIN_VALUE = 0.0;
    public static final double MAX_VALUE = 1.0;

    public MatchThreshold {
        ObjectGuard.notNull(value, "value");

        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw GuardException.forParameter("value", "must be between 0.0 and 1.0");
        }
    }

    public static Result<MatchThreshold> create(double value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            return Result.failure(PresenceErrors.INVALID_MATCH_THRESHOLD);
        }

        return Result.success(new MatchThreshold(value));
    }

    public static MatchThreshold of(double value) {
        return new MatchThreshold(value);
    }
}
