package atlas.application.routines.queries.listroutines;

import atlas.application.routines.dto.RoutineSummaryDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListRoutinesQuery(boolean includeArchived) implements Query<Result<List<RoutineSummaryDto>>> {}
