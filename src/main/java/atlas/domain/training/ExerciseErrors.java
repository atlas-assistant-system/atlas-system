package atlas.domain.training;

import atlas.domain.sharedkernel.results.Error;

public final class ExerciseErrors {

    public static final Error NAME_REQUIRED =
        Error.validation("Exercise.NameRequired", "An exercise needs a name.");

    public static final Error NAME_TOO_LONG =
        Error.validation("Exercise.NameTooLong", "The name is too long.");

    public static final Error ALREADY_ARCHIVED =
        Error.conflict("Exercise.AlreadyArchived", "An archived exercise no longer takes changes.");

    public static final Error NOT_ARCHIVED =
        Error.conflict("Exercise.NotArchived", "That exercise is not archived.");

    public static final Error NAME_ALREADY_TAKEN = Error.conflict(
        "Exercise.NameAlreadyTaken", "There is already an exercise with that name.");

    public static final Error NAME_TAKEN_BY_ANOTHER_MEASURE = Error.conflict(
        "Exercise.NameTakenByAnotherMeasure",
        "An archived exercise holds that name under a different measure.");

    public static Error notFound(ExerciseId id) {
        return Error.notFound("Exercise.NotFound", "Exercise '" + id + "' was not found.");
    }

    private ExerciseErrors() {}
}
