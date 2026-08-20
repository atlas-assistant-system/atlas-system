package atlas.application.economy.commands.setsavingsgoal;

import atlas.application.economy.dto.SavingsGoalDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class SetSavingsGoalCommandHandler
    implements CommandHandler<SetSavingsGoalCommand, Result<SavingsGoalDto>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public SetSavingsGoalCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<SavingsGoalDto> handle(SetSavingsGoalCommand command) {
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
            var goalResult = SavingsGoal.set(
                goals.nextId(), nameResult.value(), targetResult.value(), command.deadline(), today, now);
            if (goalResult.isFailure()) {
                return Result.failure(goalResult.error());
            }

            var goal = goalResult.value();
            goals.create(goal);

            return Result.success(EconomyMapper.toDto(goal));
        });
    }
}
