package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;

public record SimilarityScore(Double value) implements SingleValueObject<Double> {

    public static final double MIN_VALUE = 0.0;
    public static final double MAX_VALUE = 1.0;

    public SimilarityScore {
        ObjectGuard.notNull(value, "value");

        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw GuardException.forParameter("value", "must be between 0.0 and 1.0");
        }
    }

    public static Result<SimilarityScore> create(double value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            return Result.failure(PresenceErrors.INVALID_SIMILARITY_SCORE);
        }

        return Result.success(new SimilarityScore(value));
    }

    public static SimilarityScore of(double value) {
        return new SimilarityScore(value);
    }

    public boolean meets(MatchThreshold threshold) {
        return value >= threshold.value();
    }
}
