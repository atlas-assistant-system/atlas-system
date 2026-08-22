package atlas.application.training.queries.gettodayworkout;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

/** Puede haber cero, uno o dos: manana y tarde son dos entrenos del mismo dia. */
public record GetTodayWorkoutQuery() implements Query<Result<List<WorkoutLogDto>>> {}
