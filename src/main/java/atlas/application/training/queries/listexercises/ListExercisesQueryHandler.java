package atlas.application.training.queries.listexercises;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.ExerciseReadModel;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class ListExercisesQueryHandler
    implements QueryHandler<ListExercisesQuery, Result<List<ExerciseDto>>> {

    private final ExerciseReadModel exercises;

    public ListExercisesQueryHandler(ExerciseReadModel exercises) {
        this.exercises = exercises;
    }

    @Override
    public Result<List<ExerciseDto>> handle(ListExercisesQuery query) {
        return Result.success(TrainingMapper.toExercises(exercises.findAll(query.includeArchived())));
    }
}
