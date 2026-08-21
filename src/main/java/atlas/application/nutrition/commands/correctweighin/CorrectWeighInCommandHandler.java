package atlas.application.nutrition.commands.correctweighin;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.WeighInErrors;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class CorrectWeighInCommandHandler
    implements CommandHandler<CorrectWeighInCommand, Result<WeighInDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public CorrectWeighInCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WeighInDto> handle(CorrectWeighInCommand command) {
        var weightResult = Weight.ofKilograms(command.weight());
        if (weightResult.isFailure()) {
            return Result.failure(weightResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var weighIns = unitOfWork.weighIns();
            var found = weighIns.get(command.weighInId());
            if (found.isEmpty()) {
                return Result.failure(WeighInErrors.notFound(command.weighInId()));
            }

            var weighIn = found.get();
            var corrected = weighIn.correct(weightResult.value(), now);
            if (corrected.isFailure()) {
                return Result.failure(corrected.error());
            }

            weighIns.update(weighIn);

            return Result.success(NutritionMapper.toDto(weighIn));
        });
    }
}
