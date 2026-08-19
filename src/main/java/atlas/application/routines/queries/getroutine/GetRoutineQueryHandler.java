package atlas.application.routines.queries.getroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetRoutineQueryHandler implements QueryHandler<GetRoutineQuery, Result<RoutineDto>> {

    private final RoutineReadModel routines;

    public GetRoutineQueryHandler(RoutineReadModel routines) {
        this.routines = routines;
    }

    @Override
    public Result<RoutineDto> handle(GetRoutineQuery query) {
        return routines
            .find(query.routineId())
            .map(routine -> Result.success(RoutineMapper.toDto(routine)))
            .orElseGet(() -> Result.failure(RoutineErrors.notFound(query.routineId())));
    }
}
