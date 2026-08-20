package atlas.application.economy.commands.recategorizemovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.MovementErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class RecategorizeMovementCommandHandler
    implements CommandHandler<RecategorizeMovementCommand, Result<MovementDto>> {

    private final MovementUnitOfWork unitOfWork;
    private final Clock clock;

    public RecategorizeMovementCommandHandler(MovementUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<MovementDto> handle(RecategorizeMovementCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var movements = unitOfWork.movements();
            var found = movements.get(command.movementId());
            if (found.isEmpty()) {
                return Result.failure(MovementErrors.notFound(command.movementId()));
            }

            var movement = found.get();
            var recategorized = movement.recategorize(command.category(), now);
            if (recategorized.isFailure()) {
                return Result.failure(recategorized.error());
            }

            movements.update(movement);

            return Result.success(EconomyMapper.toDto(movement));
        });
    }
}
