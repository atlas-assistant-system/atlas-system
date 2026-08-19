package atlas.application.routines.queries.gettoday;

import atlas.application.routines.dto.TodayRoutineDto;
import java.util.List;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetTodayQuery() implements Query<Result<List<TodayRoutineDto>>> {}
