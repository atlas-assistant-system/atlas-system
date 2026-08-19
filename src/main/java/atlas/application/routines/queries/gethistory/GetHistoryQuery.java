package atlas.application.routines.queries.gethistory;

import atlas.application.routines.dto.HistoryDayDto;
import atlas.domain.routines.RoutineId;
import java.time.LocalDate;
import java.util.List;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetHistoryQuery(RoutineId routineId, LocalDate from, LocalDate to)
    implements Query<Result<List<HistoryDayDto>>> {}
