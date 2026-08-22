package atlas.domain.nutrition;

import atlas.domain.sharedkernel.results.Error;

public final class IntakeErrors {

    public static final Error CALORIES_REQUIRED =
        Error.validation("Intake.CaloriesRequired", "An intake of zero calories is not an intake.");

    public static final Error CANNOT_BE_DATED_IN_THE_FUTURE = Error.validation(
        "Intake.CannotBeDatedInTheFuture", "An intake records something you already ate.");

    public static final Error NOTE_TOO_LONG =
        Error.validation("Intake.NoteTooLong", "The note is too long.");

    public static Error notFound(IntakeId id) {
        return Error.notFound("Intake.NotFound", "Intake '" + id + "' was not found.");
    }

    private IntakeErrors() {}
}
