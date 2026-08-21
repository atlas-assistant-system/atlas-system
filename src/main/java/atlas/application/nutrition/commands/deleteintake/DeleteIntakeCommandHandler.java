package atlas.application.nutrition.commands.deleteintake;

import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class DeleteIntakeCommandHandler implements CommandHandler<DeleteIntakeCommand, Result<Void>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteIntakeCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteIntakeCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var intakes = unitOfWork.intakes();
            var found = intakes.get(command.intakeId());
            if (found.isEmpty()) {
                return Result.failure(IntakeErrors.notFound(command.intakeId()));
            }

            var intake = found.get();
            var deleted = intake.delete(now);
            if (deleted.isFailure()) {
                return deleted;
            }

            intakes.delete(intake);

            return Result.success();
        });
    }
}
