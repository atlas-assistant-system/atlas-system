package atlas.application.routines.queries.getstreak;

import atlas.application.routines.dto.StreakDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record GetStreakQuery(RoutineId routineId) implements Query<Result<StreakDto>> {}
