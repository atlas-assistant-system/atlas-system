package atlas.application.economy.commands.abandonsavingsgoal;

import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.SavingsGoalErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class AbandonSavingsGoalCommandHandler
    implements CommandHandler<AbandonSavingsGoalCommand, Result<Void>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public AbandonSavingsGoalCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(AbandonSavingsGoalCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var goals = unitOfWork.goals();
            var found = goals.get(command.goalId());
            if (found.isEmpty()) {
                return Result.failure(SavingsGoalErrors.notFound(command.goalId()));
            }

            var goal = found.get();
            var abandoned = goal.abandon(now);
            if (abandoned.isFailure()) {
                return Result.failure(abandoned.error());
            }

            goals.delete(goal);

            return Result.success();
        });
    }
}
