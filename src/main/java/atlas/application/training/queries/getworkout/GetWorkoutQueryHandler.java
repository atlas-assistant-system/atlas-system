package atlas.application.training.queries.getworkout;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.WorkoutReadModel;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;

public final class GetWorkoutQueryHandler
    implements QueryHandler<GetWorkoutQuery, Result<WorkoutDto>> {

    private final WorkoutReadModel workouts;

    public GetWorkoutQueryHandler(WorkoutReadModel workouts) {
        this.workouts = workouts;
    }

    @Override
    public Result<WorkoutDto> handle(GetWorkoutQuery query) {
        return workouts.find(query.workoutId())
            .map(workout -> Result.success(TrainingMapper.toDto(workout)))
            .orElseGet(() -> Result.failure(WorkoutErrors.notFound(query.workoutId())));
    }
}
