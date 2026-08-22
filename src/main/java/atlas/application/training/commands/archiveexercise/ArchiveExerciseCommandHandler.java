package atlas.application.training.commands.archiveexercise;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseErrors;
import java.time.Clock;

public final class ArchiveExerciseCommandHandler implements CommandHandler<ArchiveExerciseCommand, Result<Void>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public ArchiveExerciseCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ArchiveExerciseCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var exercises = unitOfWork.exercises();
            var found = exercises.get(command.exerciseId());
            if (found.isEmpty()) {
                return Result.failure(ExerciseErrors.notFound(command.exerciseId()));
            }

            var exercise = found.get();
            var changed = exercise.archive(now);
            if (changed.isFailure()) {
                return changed;
            }

            exercises.update(exercise);

            return Result.success();
        });
    }
}
