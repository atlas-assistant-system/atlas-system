package atlas.application.training.queries.getworkoutlog;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogErrors;

public final class GetWorkoutLogQueryHandler
    implements QueryHandler<GetWorkoutLogQuery, Result<WorkoutLogDto>> {

    private final WorkoutLogReadModel logs;

    public GetWorkoutLogQueryHandler(WorkoutLogReadModel logs) {
        this.logs = logs;
    }

    @Override
    public Result<WorkoutLogDto> handle(GetWorkoutLogQuery query) {
        return logs.find(query.logId())
            .map(log -> Result.success(TrainingMapper.toDto(log)))
            .orElseGet(() -> Result.failure(WorkoutLogErrors.notFound(query.logId())));
    }
}
