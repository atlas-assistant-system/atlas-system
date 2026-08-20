package atlas.domain.economy;

import atlas.domain.sharedkernel.results.Error;

public final class MovementErrors {

    public static final Error AMOUNT_MUST_BE_POSITIVE =
        Error.validation("Movement.AmountMustBePositive", "The amount must be greater than zero.");

    public static final Error NOTE_TOO_LONG =
        Error.validation("Movement.NoteTooLong", "The note is too long.");

    public static final Error CATEGORY_DOES_NOT_MATCH_KIND = Error.validation(
        "Movement.CategoryDoesNotMatchKind", "The category does not belong to this kind of movement.");

    public static final Error CANNOT_BE_DATED_IN_THE_FUTURE = Error.validation(
        "Movement.CannotBeDatedInTheFuture", "A movement records something that already happened.");

    public static Error notFound(MovementId id) {
        return Error.notFound("Movement.NotFound", "Movement '" + id + "' was not found.");
    }

    private MovementErrors() {}
}
