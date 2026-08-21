package atlas.application.nutrition.commands.deleteweighin;

import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.WeighInErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class DeleteWeighInCommandHandler implements CommandHandler<DeleteWeighInCommand, Result<Void>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteWeighInCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteWeighInCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var weighIns = unitOfWork.weighIns();
            var found = weighIns.get(command.weighInId());
            if (found.isEmpty()) {
                return Result.failure(WeighInErrors.notFound(command.weighInId()));
            }

            var weighIn = found.get();
            var deleted = weighIn.delete(now);
            if (deleted.isFailure()) {
                return deleted;
            }

            weighIns.delete(weighIn);

            return Result.success();
        });
    }
}
