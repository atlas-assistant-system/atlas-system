package atlas.application.routines.queries.getroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record GetRoutineQuery(RoutineId routineId) implements Query<Result<RoutineDto>> {}
