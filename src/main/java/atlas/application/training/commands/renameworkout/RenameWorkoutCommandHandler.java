package atlas.application.training.commands.renameworkout;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.vos.WorkoutName;
import java.time.Clock;

public final class RenameWorkoutCommandHandler
    implements CommandHandler<RenameWorkoutCommand, Result<WorkoutDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public RenameWorkoutCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WorkoutDto> handle(RenameWorkoutCommand command) {
        var nameResult = WorkoutName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var workouts = unitOfWork.workouts();
            var found = workouts.get(command.workoutId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutErrors.notFound(command.workoutId()));
            }

            var workout = found.get();
            var renamed = workout.rename(nameResult.value(), now);
            if (renamed.isFailure()) {
                return Result.failure(renamed.error());
            }

            workouts.update(workout);

            return Result.success(TrainingMapper.toDto(workout));
        });
    }
}
