package atlas.application.economy.commands.changebudgetlimit;

import atlas.application.economy.dto.BudgetDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.BudgetErrors;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class ChangeBudgetLimitCommandHandler
    implements CommandHandler<ChangeBudgetLimitCommand, Result<BudgetDto>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public ChangeBudgetLimitCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<BudgetDto> handle(ChangeBudgetLimitCommand command) {
        var limitResult = Money.ofEuros(command.limit());
        if (limitResult.isFailure()) {
            return Result.failure(limitResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var budgets = unitOfWork.budgets();
            var found = budgets.get(command.budgetId());
            if (found.isEmpty()) {
                return Result.failure(BudgetErrors.notFound(command.budgetId()));
            }

            var budget = found.get();
            var changed = budget.changeLimit(limitResult.value(), now);
            if (changed.isFailure()) {
                return Result.failure(changed.error());
            }

            budgets.update(budget);

            return Result.success(EconomyMapper.toDto(budget));
        });
    }
}
