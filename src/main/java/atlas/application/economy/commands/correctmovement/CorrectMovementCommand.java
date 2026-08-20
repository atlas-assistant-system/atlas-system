package atlas.application.economy.commands.correctmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.MovementId;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CorrectMovementCommand(
    MovementId movementId,
    BigDecimal amount,
    String note,
    LocalDate occurredOn) implements Command<Result<MovementDto>> {}
