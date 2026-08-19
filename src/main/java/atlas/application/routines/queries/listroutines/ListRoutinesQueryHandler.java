package atlas.application.routines.queries.listroutines;

import atlas.application.routines.dto.RoutineSummaryDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class ListRoutinesQueryHandler
    implements QueryHandler<ListRoutinesQuery, Result<List<RoutineSummaryDto>>> {

    private final RoutineReadModel routines;

    public ListRoutinesQueryHandler(RoutineReadModel routines) {
        this.routines = routines;
    }

    @Override
    public Result<List<RoutineSummaryDto>> handle(ListRoutinesQuery query) {
        return Result.success(RoutineMapper.toSummaries(routines.findAll(query.includeArchived())));
    }
}
