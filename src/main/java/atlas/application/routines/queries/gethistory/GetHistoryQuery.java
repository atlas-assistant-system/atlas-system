package atlas.application.routines.queries.gethistory;

import atlas.application.routines.dto.HistoryDayDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record GetHistoryQuery(RoutineId routineId, LocalDate from, LocalDate to)
    implements Query<Result<List<HistoryDayDto>>> {}
