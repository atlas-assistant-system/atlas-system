package atlas.application.economy.commands.changesavingsgoal;

import atlas.application.economy.dto.SavingsGoalDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.SavingsGoalErrors;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class ChangeSavingsGoalCommandHandler
    implements CommandHandler<ChangeSavingsGoalCommand, Result<SavingsGoalDto>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public ChangeSavingsGoalCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<SavingsGoalDto> handle(ChangeSavingsGoalCommand command) {
        var nameResult = GoalName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var targetResult = Money.ofEuros(command.target());
        if (targetResult.isFailure()) {
            return Result.failure(targetResult.error());
        }

        var today = LocalDate.now(clock);
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var goals = unitOfWork.goals();
            var found = goals.get(command.goalId());
            if (found.isEmpty()) {
                return Result.failure(SavingsGoalErrors.notFound(command.goalId()));
            }

            var goal = found.get();
            var changed = goal.change(
                nameResult.value(), targetResult.value(), command.deadline(), today, now);
            if (changed.isFailure()) {
                return Result.failure(changed.error());
            }

            goals.update(goal);

            return Result.success(EconomyMapper.toDto(goal));
        });
    }
}
