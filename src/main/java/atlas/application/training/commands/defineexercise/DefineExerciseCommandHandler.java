package atlas.application.training.commands.defineexercise;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseErrors;
import atlas.domain.training.vos.ExerciseName;
import java.time.Clock;
import java.time.Instant;

public final class DefineExerciseCommandHandler
    implements CommandHandler<DefineExerciseCommand, Result<ExerciseDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public DefineExerciseCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<ExerciseDto> handle(DefineExerciseCommand command) {
        var nameResult = ExerciseName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var exercises = unitOfWork.exercises();
            var sameName = exercises.findByName(nameResult.value());
            if (sameName.isPresent()) {
                return bringBack(sameName.get(), command, exercises, now);
            }

            var defined = Exercise.define(
                exercises.nextId(), nameResult.value(), command.metric(), now);
            if (defined.isFailure()) {
                return Result.failure(defined.error());
            }

            var exercise = defined.value();
            exercises.create(exercise);

            return Result.success(TrainingMapper.toDto(exercise));
        });
    }

    private static Result<ExerciseDto> bringBack(
        Exercise sameName, DefineExerciseCommand command, ExerciseRepository exercises,
        Instant now) {

        if (!sameName.isArchived()) {
            return Result.failure(ExerciseErrors.NAME_ALREADY_TAKEN);
        }

        if (sameName.metric() != command.metric()) {
            return Result.failure(ExerciseErrors.NAME_TAKEN_BY_ANOTHER_MEASURE);
        }

        var restored = sameName.unarchive(now);
        if (restored.isFailure()) {
            return Result.failure(restored.error());
        }

        exercises.update(sameName);

        return Result.success(TrainingMapper.toDto(sameName));
    }
}
