package atlas.application.training.commands.scheduleworkout;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;
import java.time.Clock;


public final class ScheduleWorkoutCommandHandler
    implements CommandHandler<ScheduleWorkoutCommand, Result<WorkoutDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public ScheduleWorkoutCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WorkoutDto> handle(ScheduleWorkoutCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var workouts = unitOfWork.workouts();
            var found = workouts.get(command.workoutId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutErrors.notFound(command.workoutId()));
            }

            var workout = found.get();
            var scheduled = workout.scheduleOn(command.days(), now);
            if (scheduled.isFailure()) {
                return Result.failure(scheduled.error());
            }

            workouts.update(workout);

            return Result.success(TrainingMapper.toDto(workout));
        });
    }
}
