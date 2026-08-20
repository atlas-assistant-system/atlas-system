package atlas.application.economy.commands.definebudget;

import atlas.application.economy.dto.BudgetDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetErrors;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class DefineBudgetCommandHandler implements CommandHandler<DefineBudgetCommand, Result<BudgetDto>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public DefineBudgetCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<BudgetDto> handle(DefineBudgetCommand command) {
        var limitResult = Money.ofEuros(command.limit());
        if (limitResult.isFailure()) {
            return Result.failure(limitResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var budgets = unitOfWork.budgets();
            if (budgets.existsFor(command.category())) {
                return Result.failure(BudgetErrors.alreadyDefinedFor(command.category()));
            }

            var defined = Budget.define(budgets.nextId(), command.category(), limitResult.value(), now);
            if (defined.isFailure()) {
                return Result.failure(defined.error());
            }

            var budget = defined.value();
            budgets.create(budget);

            return Result.success(EconomyMapper.toDto(budget));
        });
    }
}
