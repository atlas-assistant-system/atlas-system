package atlas.domain.economy;

import atlas.domain.economy.enums.Category;
import atlas.domain.sharedkernel.results.Error;

public final class BudgetErrors {

    public static final Error ONLY_SPENDING_CAN_BE_BUDGETED = Error.validation(
        "Budget.OnlySpendingCanBeBudgeted", "Only a spending category can have a budget.");

    public static final Error LIMIT_UNCHANGED =
        Error.conflict("Budget.LimitUnchanged", "The budget already has that limit.");

    public static Error alreadyDefinedFor(Category category) {
        return Error.conflict(
            "Budget.AlreadyDefined", "Category '" + category.name() + "' already has a budget.");
    }

    public static Error notFound(BudgetId id) {
        return Error.notFound("Budget.NotFound", "Budget '" + id + "' was not found.");
    }

    private BudgetErrors() {}
}
