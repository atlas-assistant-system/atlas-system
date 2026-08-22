package atlas.application.training.commands.setworkoutplan;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.commands.PlannedLineInput;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.IdGenerator;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseErrors;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.PlannedLine;
import atlas.domain.training.vos.SetCount;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Fija el plan completo. Comprueba contra el catálogo que cada ejercicio existe y no está
 * archivado antes de tocar la plantilla: una línea que apunta a nada rompería el histórico
 * en cuanto se desplegase en un entreno.
 */
public final class SetWorkoutPlanCommandHandler
    implements CommandHandler<SetWorkoutPlanCommand, Result<WorkoutDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;
    private final IdGenerator ids;

    public SetWorkoutPlanCommandHandler(
        TrainingUnitOfWork unitOfWork, Clock clock, IdGenerator ids) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public Result<WorkoutDto> handle(SetWorkoutPlanCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var workouts = unitOfWork.workouts();
            var found = workouts.get(command.workoutId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutErrors.notFound(command.workoutId()));
            }

            var lines = new ArrayList<PlannedLine>(command.lines().size());
            for (var input : command.lines()) {
                var line = toLine(input, unitOfWork.exercises());
                if (line.isFailure()) {
                    return Result.failure(line.error());
                }

                lines.add(line.value());
            }

            var workout = found.get();
            var changed = workout.setPlan(List.copyOf(lines), now);
            if (changed.isFailure()) {
                return Result.failure(changed.error());
            }

            workouts.update(workout);

            return Result.success(TrainingMapper.toDto(workout));
        });
    }

    private Result<PlannedLine> toLine(PlannedLineInput input, ExerciseRepository exercises) {
        var exercise = exercises.get(input.exerciseId());
        if (exercise.isEmpty()) {
            return Result.failure(ExerciseErrors.notFound(input.exerciseId()));
        }

        if (exercise.get().isArchived()) {
            return Result.failure(WorkoutErrors.ARCHIVED_EXERCISE_NOT_ALLOWED);
        }

        var sets = SetCount.create(input.sets());
        if (sets.isFailure()) {
            return Result.failure(sets.error());
        }

        var target = input.target();
        var effort = Effort.ofKilograms(
            target.load(), target.reps(), target.seconds(), target.meters());
        if (effort.isFailure()) {
            return Result.failure(effort.error());
        }

        return Result.success(new PlannedLine(
            PlannedExerciseId.of(ids.next()), input.exerciseId(), sets.value(), effort.value()));
    }
}
