package atlas.application.routines.queries.getperiodprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record GetPeriodProgressQuery(RoutineId routineId, LocalDate dayInPeriod)
    implements Query<Result<PeriodProgressDto>> {}
