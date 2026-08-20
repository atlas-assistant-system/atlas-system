package atlas.application.economy.queries.getmovement;

import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.economy.MovementErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetMovementQueryHandler implements QueryHandler<GetMovementQuery, Result<MovementDto>> {

    private final MovementReadModel movements;

    public GetMovementQueryHandler(MovementReadModel movements) {
        this.movements = movements;
    }

    @Override
    public Result<MovementDto> handle(GetMovementQuery query) {
        return movements.find(query.movementId())
            .map(movement -> Result.success(EconomyMapper.toDto(movement)))
            .orElseGet(() -> Result.failure(MovementErrors.notFound(query.movementId())));
    }
}
