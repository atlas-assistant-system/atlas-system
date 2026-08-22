package atlas.application.training.commands.discardworkoutlog;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogErrors;
import java.time.Clock;

public final class DiscardWorkoutLogCommandHandler
    implements CommandHandler<DiscardWorkoutLogCommand, Result<Void>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public DiscardWorkoutLogCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DiscardWorkoutLogCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var logs = unitOfWork.logs();
            var found = logs.get(command.logId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutLogErrors.notFound(command.logId()));
            }

            var log = found.get();
            var discarded = log.discard(now);
            if (discarded.isFailure()) {
                return discarded;
            }

            logs.delete(log);

            return Result.success();
        });
    }
}
