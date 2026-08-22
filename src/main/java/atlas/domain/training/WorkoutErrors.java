package atlas.domain.training;

import atlas.domain.sharedkernel.results.Error;

public final class WorkoutErrors {

    public static final Error NAME_REQUIRED =
        Error.validation("Workout.NameRequired", "A workout needs a name.");

    public static final Error NAME_TOO_LONG =
        Error.validation("Workout.NameTooLong", "The name is too long.");

    public static final Error SET_COUNT_OUT_OF_RANGE =
        Error.validation("Workout.SetCountOutOfRange", "That is not a number of sets anybody does.");

    public static final Error ALREADY_ARCHIVED =
        Error.conflict("Workout.AlreadyArchived", "An archived workout no longer takes changes.");

    public static final Error ARCHIVED_EXERCISE_NOT_ALLOWED = Error.validation(
        "Workout.ArchivedExerciseNotAllowed", "An archived exercise cannot go into a plan.");

    public static Error notFound(WorkoutId id) {
        return Error.notFound("Workout.NotFound", "Workout '" + id + "' was not found.");
    }

    private WorkoutErrors() {}
}
