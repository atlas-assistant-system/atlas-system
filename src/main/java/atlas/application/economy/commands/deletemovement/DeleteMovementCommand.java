package atlas.application.economy.commands.deletemovement;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.MovementId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteMovementCommand(MovementId movementId) implements Command<Result<Void>> {}
