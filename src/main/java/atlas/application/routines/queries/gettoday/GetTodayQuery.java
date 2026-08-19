package atlas.application.routines.queries.gettoday;

import atlas.application.routines.dto.TodayRoutineDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record GetTodayQuery() implements Query<Result<List<TodayRoutineDto>>> {}
