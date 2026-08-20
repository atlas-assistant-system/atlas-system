package atlas.application.economy.commands.deletemovement;

import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.MovementErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class DeleteMovementCommandHandler implements CommandHandler<DeleteMovementCommand, Result<Void>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteMovementCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteMovementCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var movements = unitOfWork.movements();
            var found = movements.get(command.movementId());
            if (found.isEmpty()) {
                return Result.failure(MovementErrors.notFound(command.movementId()));
            }

            var movement = found.get();
            var deleted = movement.delete(now);
            if (deleted.isFailure()) {
                return Result.failure(deleted.error());
            }

            movements.delete(movement);

            return Result.success();
        });
    }
}
