package atlas.application.training.queries.listworkouts;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListWorkoutsQuery(boolean includeArchived) implements Query<Result<List<WorkoutDto>>> {}
