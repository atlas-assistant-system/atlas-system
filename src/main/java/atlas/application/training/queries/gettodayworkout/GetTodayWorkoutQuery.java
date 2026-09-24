package atlas.application.training.queries.gettodayworkout;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record GetTodayWorkoutQuery() implements Query<Result<List<WorkoutLogDto>>> {}
