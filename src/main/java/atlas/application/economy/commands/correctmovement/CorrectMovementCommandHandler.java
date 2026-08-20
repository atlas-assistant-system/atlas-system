package atlas.application.economy.commands.correctmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class CorrectMovementCommandHandler
    implements CommandHandler<CorrectMovementCommand, Result<MovementDto>> {

    private final MovementUnitOfWork unitOfWork;
    private final Clock clock;

    public CorrectMovementCommandHandler(MovementUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<MovementDto> handle(CorrectMovementCommand command) {
        var amountResult = Money.ofEuros(command.amount());
        if (amountResult.isFailure()) {
            return Result.failure(amountResult.error());
        }

        var noteResult = MovementNote.create(command.note());
        if (noteResult.isFailure()) {
            return Result.failure(noteResult.error());
        }

        var today = LocalDate.now(clock);
        var occurredOn = command.occurredOn() == null ? today : command.occurredOn();
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var movements = unitOfWork.movements();
            var found = movements.get(command.movementId());
            if (found.isEmpty()) {
                return Result.failure(MovementErrors.notFound(command.movementId()));
            }

            var movement = found.get();
            var corrected = movement.correct(amountResult.value(), noteResult.value(), occurredOn, today, now);
            if (corrected.isFailure()) {
                return Result.failure(corrected.error());
            }

            movements.update(movement);

            return Result.success(EconomyMapper.toDto(movement));
        });
    }
}
