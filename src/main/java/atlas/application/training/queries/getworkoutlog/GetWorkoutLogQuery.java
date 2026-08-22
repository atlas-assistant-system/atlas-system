package atlas.application.training.queries.getworkoutlog;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogId;

public record GetWorkoutLogQuery(WorkoutLogId logId) implements Query<Result<WorkoutLogDto>> {}
