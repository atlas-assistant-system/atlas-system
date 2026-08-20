package atlas.application.economy.commands.recordmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordMovementCommand(
    MovementKind kind,
    BigDecimal amount,
    Category category,
    String note,
    LocalDate occurredOn) implements Command<Result<MovementDto>> {}
