package atlas.application.nutrition.commands.recordintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class RecordIntakeCommandHandler implements CommandHandler<RecordIntakeCommand, Result<IntakeDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public RecordIntakeCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<IntakeDto> handle(RecordIntakeCommand command) {
        var macrosResult = Macros.create(command.protein(), command.carbs(), command.fat());
        if (macrosResult.isFailure()) {
            return Result.failure(macrosResult.error());
        }

        var noteResult = IntakeNote.create(command.note());
        if (noteResult.isFailure()) {
            return Result.failure(noteResult.error());
        }

        var today = LocalDate.now(clock);
        var consumedOn = command.consumedOn() == null ? today : command.consumedOn();
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var intakes = unitOfWork.intakes();
            var recorded = Intake.record(
                intakes.nextId(), macrosResult.value(), noteResult.value(), consumedOn, today, now);
            if (recorded.isFailure()) {
                return Result.failure(recorded.error());
            }

            var intake = recorded.value();
            intakes.create(intake);

            return Result.success(NutritionMapper.toDto(intake));
        });
    }
}
