package atlas.domain.routines;

import atlas.domain.sharedkernel.results.Error;
import java.time.LocalDate;

public final class RoutineErrors {

    public static final Error NAME_REQUIRED =
        Error.validation("Routine.NameRequired", "A name is required.");

    public static final Error NAME_TOO_LONG =
        Error.validation("Routine.NameTooLong", "The name is too long.");

    public static final Error DESCRIPTION_TOO_LONG =
        Error.validation("Routine.DescriptionTooLong", "The description is too long.");

    public static final Error TARGET_MUST_BE_POSITIVE =
        Error.validation("Routine.TargetMustBePositive", "The target must be greater than zero.");

    public static final Error AMOUNT_MUST_BE_POSITIVE =
        Error.validation("Routine.AmountMustBePositive", "The logged amount must be greater than zero.");

    public static final Error UNIT_TOO_LONG =
        Error.validation("Routine.UnitTooLong", "The unit is too long.");

    public static final Error ACTIVE_DAYS_REQUIRED_FOR_DAILY_ROUTINE = Error.validation(
        "Routine.ActiveDaysRequiredForDailyRoutine", "A daily routine needs at least one active weekday.");

    public static final Error ACTIVE_DAYS_NOT_ALLOWED_FOR_THIS_PERIOD = Error.validation(
        "Routine.ActiveDaysNotAllowedForThisPeriod", "Only a daily routine can restrict weekdays.");

    public static final Error DAYS_OF_MONTH_NOT_ALLOWED_FOR_THIS_PERIOD = Error.validation(
        "Routine.DaysOfMonthNotAllowedForThisPeriod", "Only a monthly routine can restrict days of the month.");

    public static final Error DAY_OF_MONTH_OUT_OF_RANGE = Error.validation(
        "Routine.DayOfMonthOutOfRange", "A day of the month is 1 to 31, or 0 for the last day of the month.");

    public static final Error ROUTINE_IS_ARCHIVED =
        Error.conflict("Routine.IsArchived", "An archived routine accepts no changes or new entries.");

    public static final Error ROUTINE_ALREADY_ARCHIVED =
        Error.conflict("Routine.AlreadyArchived", "The routine is already archived.");

    public static final Error ROUTINE_NOT_ARCHIVED =
        Error.conflict("Routine.NotArchived", "Only an archived routine can be unarchived.");

    public static final Error DAY_NOT_SCHEDULED =
        Error.validation("Routine.DayNotScheduled", "The routine is not scheduled on that day.");

    public static Error notFound(RoutineId id) {
        return Error.notFound("Routine.NotFound", "Routine '" + id + "' was not found.");
    }

    public static Error entryNotFound(RoutineId id, LocalDate day) {
        return Error.notFound("Routine.EntryNotFound", "Routine '" + id + "' has no entry for " + day + ".");
    }

    private RoutineErrors() {}
}
