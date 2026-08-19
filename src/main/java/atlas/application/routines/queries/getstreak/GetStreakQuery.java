package atlas.application.routines.queries.getstreak;

import atlas.application.routines.dto.StreakDto;
import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetStreakQuery(RoutineId routineId) implements Query<Result<StreakDto>> {}
