package atlas.application.routines.queries.getperiodprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.domain.routines.RoutineId;
import java.time.LocalDate;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetPeriodProgressQuery(RoutineId routineId, LocalDate dayInPeriod)
    implements Query<Result<PeriodProgressDto>> {}
