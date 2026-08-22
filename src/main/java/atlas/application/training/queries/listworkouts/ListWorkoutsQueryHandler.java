package atlas.application.training.queries.listworkouts;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.WorkoutReadModel;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class ListWorkoutsQueryHandler
    implements QueryHandler<ListWorkoutsQuery, Result<List<WorkoutDto>>> {

    private final WorkoutReadModel workouts;

    public ListWorkoutsQueryHandler(WorkoutReadModel workouts) {
        this.workouts = workouts;
    }

    @Override
    public Result<List<WorkoutDto>> handle(ListWorkoutsQuery query) {
        return Result.success(TrainingMapper.toWorkouts(workouts.findAll(query.includeArchived())));
    }
}
