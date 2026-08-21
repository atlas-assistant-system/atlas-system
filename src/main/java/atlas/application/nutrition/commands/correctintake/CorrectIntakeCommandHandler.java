package atlas.application.nutrition.commands.correctintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class CorrectIntakeCommandHandler implements CommandHandler<CorrectIntakeCommand, Result<IntakeDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public CorrectIntakeCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<IntakeDto> handle(CorrectIntakeCommand command) {
        var macrosResult = Macros.create(command.protein(), command.carbs(), command.fat());
        if (macrosResult.isFailure()) {
            return Result.failure(macrosResult.error());
        }

        var noteResult = IntakeNote.create(command.note());
        if (noteResult.isFailure()) {
            return Result.failure(noteResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var intakes = unitOfWork.intakes();
            var found = intakes.get(command.intakeId());
            if (found.isEmpty()) {
                return Result.failure(IntakeErrors.notFound(command.intakeId()));
            }

            var intake = found.get();
            var corrected = intake.correct(macrosResult.value(), noteResult.value(), now);
            if (corrected.isFailure()) {
                return Result.failure(corrected.error());
            }

            intakes.update(intake);

            return Result.success(NutritionMapper.toDto(intake));
        });
    }
}
