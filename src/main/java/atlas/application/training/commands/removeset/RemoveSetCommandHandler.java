package atlas.application.training.commands.removeset;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogErrors;
import java.time.Clock;

public final class RemoveSetCommandHandler
    implements CommandHandler<RemoveSetCommand, Result<Void>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public RemoveSetCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RemoveSetCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var logs = unitOfWork.logs();
            var found = logs.get(command.logId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutLogErrors.notFound(command.logId()));
            }

            var log = found.get();
            var removed = log.removeSet(command.setId(), now);
            if (removed.isFailure()) {
                return removed;
            }

            logs.update(log);

            return Result.success();
        });
    }
}
