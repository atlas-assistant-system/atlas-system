package atlas.application.training.commands.archiveworkout;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;
import java.time.Clock;

public final class ArchiveWorkoutCommandHandler
    implements CommandHandler<ArchiveWorkoutCommand, Result<Void>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public ArchiveWorkoutCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ArchiveWorkoutCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var workouts = unitOfWork.workouts();
            var found = workouts.get(command.workoutId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutErrors.notFound(command.workoutId()));
            }

            var workout = found.get();
            var archived = workout.archive(now);
            if (archived.isFailure()) {
                return archived;
            }

            workouts.update(workout);

            return Result.success();
        });
    }
}
