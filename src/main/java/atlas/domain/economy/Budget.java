package atlas.domain.economy;

import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.events.BudgetDefinedEvent;
import atlas.domain.economy.events.BudgetLimitChangedEvent;
import atlas.domain.economy.events.BudgetRemovedEvent;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;

public final class Budget extends AggregateRoot<BudgetId> {

    private final Category category;

    private Money limit;

    private Budget(BudgetId id, Category category, Money limit) {
        super(ObjectGuard.notNull(id, "id"));
        this.category = ObjectGuard.notNull(category, "category");
        this.limit = ObjectGuard.notNull(limit, "limit");
    }

    public static Result<Budget> define(BudgetId id, Category category, Money limit, Instant now) {
        if (!category.matches(MovementKind.EXPENSE)) {
            return Result.failure(BudgetErrors.ONLY_SPENDING_CAN_BE_BUDGETED);
        }

        var budget = new Budget(id, category, limit);
        budget.registerEvent(new BudgetDefinedEvent(id, now));

        return Result.success(budget);
    }

    public static Budget rehydrate(BudgetId id, Category category, Money limit) {
        return new Budget(id, category, limit);
    }

    public Result<Void> changeLimit(Money newLimit, Instant now) {
        if (limit.equals(newLimit)) {
            return Result.failure(BudgetErrors.LIMIT_UNCHANGED);
        }

        this.limit = ObjectGuard.notNull(newLimit, "newLimit");
        registerEvent(new BudgetLimitChangedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> remove(Instant now) {
        registerEvent(new BudgetRemovedEvent(id(), now));

        return Result.success();
    }

    public Category category() {
        return category;
    }

    public Money limit() {
        return limit;
    }
}
