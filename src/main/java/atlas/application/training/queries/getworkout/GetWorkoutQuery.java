package atlas.application.training.queries.getworkout;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;

public record GetWorkoutQuery(WorkoutId workoutId) implements Query<Result<WorkoutDto>> {}
