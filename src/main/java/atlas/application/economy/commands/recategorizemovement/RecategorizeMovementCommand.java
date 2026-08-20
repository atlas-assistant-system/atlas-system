package atlas.application.economy.commands.recategorizemovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.sharedkernel.results.Result;

public record RecategorizeMovementCommand(
    MovementId movementId, Category category) implements Command<Result<MovementDto>> {}
