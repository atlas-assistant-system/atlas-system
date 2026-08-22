package atlas.application.training.commands.defineworkout;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.Workout;
import atlas.domain.training.vos.WorkoutName;
import java.time.Clock;

public final class DefineWorkoutCommandHandler
    implements CommandHandler<DefineWorkoutCommand, Result<WorkoutDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public DefineWorkoutCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WorkoutDto> handle(DefineWorkoutCommand command) {
        var nameResult = WorkoutName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var workouts = unitOfWork.workouts();
            var defined = Workout.define(workouts.nextId(), nameResult.value(), now);
            if (defined.isFailure()) {
                return Result.failure(defined.error());
            }

            var workout = defined.value();
            workouts.create(workout);

            return Result.success(TrainingMapper.toDto(workout));
        });
    }
}
