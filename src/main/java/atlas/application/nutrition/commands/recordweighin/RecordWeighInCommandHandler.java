package atlas.application.nutrition.commands.recordweighin;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class RecordWeighInCommandHandler
    implements CommandHandler<RecordWeighInCommand, Result<WeighInDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public RecordWeighInCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WeighInDto> handle(RecordWeighInCommand command) {
        var weightResult = Weight.ofKilograms(command.weight());
        if (weightResult.isFailure()) {
            return Result.failure(weightResult.error());
        }

        var today = LocalDate.now(clock);
        var measuredOn = command.measuredOn() == null ? today : command.measuredOn();
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var weighIns = unitOfWork.weighIns();
            var alreadyToday = weighIns.findOn(measuredOn);
            if (alreadyToday.isPresent()) {
                var existing = alreadyToday.get();
                var corrected = existing.correct(weightResult.value(), now);
                if (corrected.isFailure()) {
                    return Result.failure(corrected.error());
                }

                weighIns.update(existing);

                return Result.success(NutritionMapper.toDto(existing));
            }

            var recorded = WeighIn.record(
                weighIns.nextId(), weightResult.value(), measuredOn, today, now);
            if (recorded.isFailure()) {
                return Result.failure(recorded.error());
            }

            var weighIn = recorded.value();
            weighIns.create(weighIn);

            return Result.success(NutritionMapper.toDto(weighIn));
        });
    }
}
