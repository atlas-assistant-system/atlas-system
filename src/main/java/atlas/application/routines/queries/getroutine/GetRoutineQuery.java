package atlas.application.routines.queries.getroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetRoutineQuery(RoutineId routineId) implements Query<Result<RoutineDto>> {}
