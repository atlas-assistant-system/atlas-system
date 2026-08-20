package atlas.application.economy.queries.getmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.economy.MovementId;
import atlas.domain.sharedkernel.results.Result;

public record GetMovementQuery(MovementId movementId) implements Query<Result<MovementDto>> {}
