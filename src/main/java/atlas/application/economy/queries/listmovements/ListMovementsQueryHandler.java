package atlas.application.economy.queries.listmovements;

import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class ListMovementsQueryHandler
    implements QueryHandler<ListMovementsQuery, Result<List<MovementDto>>> {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 200;

    private final MovementReadModel movements;

    public ListMovementsQueryHandler(MovementReadModel movements) {
        this.movements = movements;
    }

    @Override
    public Result<List<MovementDto>> handle(ListMovementsQuery query) {
        var found = movements.findLatest(
            query.from(), query.to(), query.category(), limitOf(query.limit()));

        return Result.success(EconomyMapper.toDtos(found));
    }

    private static int limitOf(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAX_LIMIT);
    }
}
