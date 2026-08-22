package atlas.application.training.commands.unarchiveexercise;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseErrors;
import java.time.Clock;

public final class UnarchiveExerciseCommandHandler implements CommandHandler<UnarchiveExerciseCommand, Result<Void>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public UnarchiveExerciseCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(UnarchiveExerciseCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var exercises = unitOfWork.exercises();
            var found = exercises.get(command.exerciseId());
            if (found.isEmpty()) {
                return Result.failure(ExerciseErrors.notFound(command.exerciseId()));
            }

            var exercise = found.get();
            var changed = exercise.unarchive(now);
            if (changed.isFailure()) {
                return changed;
            }

            exercises.update(exercise);

            return Result.success();
        });
    }
}
