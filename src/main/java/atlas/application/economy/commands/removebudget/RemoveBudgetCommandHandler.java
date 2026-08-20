package atlas.application.economy.commands.removebudget;

import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.BudgetErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class RemoveBudgetCommandHandler implements CommandHandler<RemoveBudgetCommand, Result<Void>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public RemoveBudgetCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RemoveBudgetCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var budgets = unitOfWork.budgets();
            var found = budgets.get(command.budgetId());
            if (found.isEmpty()) {
                return Result.failure(BudgetErrors.notFound(command.budgetId()));
            }

            var budget = found.get();
            var removed = budget.remove(now);
            if (removed.isFailure()) {
                return Result.failure(removed.error());
            }

            budgets.delete(budget);

            return Result.success();
        });
    }
}
