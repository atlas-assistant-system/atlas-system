package atlas.domain.nutrition;

import atlas.domain.sharedkernel.results.Error;

public final class WeighInErrors {

    public static final Error CANNOT_BE_DATED_IN_THE_FUTURE = Error.validation(
        "WeighIn.CannotBeDatedInTheFuture", "A weigh-in records a reading you already took.");

    public static Error notFound(WeighInId id) {
        return Error.notFound("WeighIn.NotFound", "Weigh-in '" + id + "' was not found.");
    }

    private WeighInErrors() {}
}
