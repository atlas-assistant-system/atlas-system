package atlas.application.training.queries.listexercises;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.ExerciseDto;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListExercisesQuery(boolean includeArchived) implements Query<Result<List<ExerciseDto>>> {}
