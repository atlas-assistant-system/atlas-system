package atlas.application.routines.queries.listroutines;

import atlas.application.routines.dto.RoutineSummaryDto;
import java.util.List;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record ListRoutinesQuery(boolean includeArchived) implements Query<Result<List<RoutineSummaryDto>>> {}
