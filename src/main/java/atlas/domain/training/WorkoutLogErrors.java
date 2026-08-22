package atlas.domain.training;

import atlas.domain.sharedkernel.results.Error;

public final class WorkoutLogErrors {

    public static final Error CANNOT_BE_DATED_IN_THE_FUTURE = Error.validation(
        "WorkoutLog.CannotBeDatedInTheFuture", "A workout log records something you already did.");

    public static final Error SET_MEASURES_NOTHING = Error.validation(
        "WorkoutLog.SetMeasuresNothing", "A set with no load, reps, seconds or metres is not a set.");

    public static final Error SET_NOT_FOUND =
        Error.notFound("WorkoutLog.SetNotFound", "That set is not in this workout.");

    public static Error notFound(WorkoutLogId id) {
        return Error.notFound("WorkoutLog.NotFound", "Workout log '" + id + "' was not found.");
    }

    private WorkoutLogErrors() {}
}
