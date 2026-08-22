package atlas.application.training.commands.renameexercise;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseErrors;
import atlas.domain.training.vos.ExerciseName;
import java.time.Clock;

public final class RenameExerciseCommandHandler
    implements CommandHandler<RenameExerciseCommand, Result<ExerciseDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public RenameExerciseCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<ExerciseDto> handle(RenameExerciseCommand command) {
        var nameResult = ExerciseName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var exercises = unitOfWork.exercises();
            var found = exercises.get(command.exerciseId());
            if (found.isEmpty()) {
                return Result.failure(ExerciseErrors.notFound(command.exerciseId()));
            }

            // Quedarse con el nombre propio no es un choque: el dueño del nombre es él mismo.
            var owner = exercises.findByName(nameResult.value());
            if (owner.isPresent() && !owner.get().id().equals(command.exerciseId())) {
                return Result.failure(ExerciseErrors.NAME_ALREADY_TAKEN);
            }

            var exercise = found.get();
            var renamed = exercise.rename(nameResult.value(), now);
            if (renamed.isFailure()) {
                return Result.failure(renamed.error());
            }

            exercises.update(exercise);

            return Result.success(TrainingMapper.toDto(exercise));
        });
    }
}
