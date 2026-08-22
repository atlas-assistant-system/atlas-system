package atlas.domain.nutrition;

import atlas.domain.sharedkernel.results.Error;

public final class PlanErrors {

    public static final Error CALORIES_REQUIRED =
        Error.validation("Plan.CaloriesRequired", "A plan without a daily calorie quota is not a plan.");

    public static final Error CANNOT_START_IN_THE_FUTURE =
        Error.validation("Plan.CannotStartInTheFuture", "A plan starts today or earlier.");

    public static final Error ALREADY_ARCHIVED =
        Error.conflict("Plan.AlreadyArchived", "An archived plan no longer takes changes.");

    public static final Error NONE_ACTIVE =
        Error.notFound("Plan.NoneActive", "There is no active plan.");

    public static Error notFound(PlanId id) {
        return Error.notFound("Plan.NotFound", "Plan '" + id + "' was not found.");
    }

    private PlanErrors() {}
}
