package atlas.application.training.commands.startworkoutlog;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.IdGenerator;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.vos.PlannedSet;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * El único handler que toca dos agregados: lee la plantilla, la despliega con
 * {@code expand()} y le pasa el guion al log, todo en la misma unidad de trabajo. La
 * traducción de plan a series vive en el dominio, no aquí.
 */
public final class StartWorkoutLogCommandHandler
    implements CommandHandler<StartWorkoutLogCommand, Result<WorkoutLogDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;
    private final IdGenerator ids;

    public StartWorkoutLogCommandHandler(
        TrainingUnitOfWork unitOfWork, Clock clock, IdGenerator ids) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public Result<WorkoutLogDto> handle(StartWorkoutLogCommand command) {
        var today = LocalDate.now(clock);
        var performedOn = command.performedOn() == null ? today : command.performedOn();
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            List<PlannedSet> plan = List.of();

            if (command.workoutId() != null) {
                var workout = unitOfWork.workouts().get(command.workoutId());
                if (workout.isEmpty()) {
                    return Result.failure(WorkoutErrors.notFound(command.workoutId()));
                }

                plan = workout.get().expand();
            }

            var logs = unitOfWork.logs();
            var started = WorkoutLog.start(
                logs.nextId(), Optional.ofNullable(command.workoutId()), plan,
                () -> SetLogId.of(ids.next()), performedOn, today, now);
            if (started.isFailure()) {
                return Result.failure(started.error());
            }

            var log = started.value();
            logs.create(log);

            return Result.success(TrainingMapper.toDto(log));
        });
    }
}
