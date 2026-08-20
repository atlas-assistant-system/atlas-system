package atlas.domain.economy;

import atlas.domain.sharedkernel.results.Error;

public final class SavingsGoalErrors {

    public static final Error NAME_REQUIRED =
        Error.validation("SavingsGoal.NameRequired", "A name is required.");

    public static final Error NAME_TOO_LONG =
        Error.validation("SavingsGoal.NameTooLong", "The name is too long.");

    public static final Error DEADLINE_MUST_BE_AHEAD = Error.validation(
        "SavingsGoal.DeadlineMustBeAhead", "The deadline has to be later than today.");

    public static Error notFound(SavingsGoalId id) {
        return Error.notFound("SavingsGoal.NotFound", "Savings goal '" + id + "' was not found.");
    }

    private SavingsGoalErrors() {}
}
