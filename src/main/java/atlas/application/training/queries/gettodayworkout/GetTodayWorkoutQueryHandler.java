package atlas.application.training.queries.gettodayworkout;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GetTodayWorkoutQueryHandler
    implements QueryHandler<GetTodayWorkoutQuery, Result<List<WorkoutLogDto>>> {

    private final WorkoutLogReadModel logs;
    private final Clock clock;

    public GetTodayWorkoutQueryHandler(WorkoutLogReadModel logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @Override
    public Result<List<WorkoutLogDto>> handle(GetTodayWorkoutQuery query) {
        return Result.success(TrainingMapper.toLogs(logs.findOn(LocalDate.now(clock))));
    }
}
