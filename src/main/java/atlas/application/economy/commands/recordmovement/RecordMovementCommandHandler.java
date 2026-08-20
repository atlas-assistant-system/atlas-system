package atlas.application.economy.commands.recordmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.economy.Movement;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class RecordMovementCommandHandler implements CommandHandler<RecordMovementCommand, Result<MovementDto>> {

    private final EconomyUnitOfWork unitOfWork;
    private final Clock clock;

    public RecordMovementCommandHandler(EconomyUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<MovementDto> handle(RecordMovementCommand command) {
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
            var recorded = Movement.record(
                movements.nextId(), command.kind(), amountResult.value(), command.category(),
                noteResult.value(), occurredOn, today, now);
            if (recorded.isFailure()) {
                return Result.failure(recorded.error());
            }

            var movement = recorded.value();
            movements.create(movement);

            return Result.success(EconomyMapper.toDto(movement));
        });
    }
}
