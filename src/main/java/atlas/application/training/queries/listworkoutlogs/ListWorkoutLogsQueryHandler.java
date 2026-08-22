package atlas.application.training.queries.listworkoutlogs;

import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.application.training.queries.Period;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ListWorkoutLogsQueryHandler
    implements QueryHandler<ListWorkoutLogsQuery, Result<List<WorkoutLogDto>>> {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final WorkoutLogReadModel logs;
    private final Clock clock;

    public ListWorkoutLogsQueryHandler(WorkoutLogReadModel logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @Override
    public Result<List<WorkoutLogDto>> handle(ListWorkoutLogsQuery query) {
        var period = Period.of(query.from(), query.to(), LocalDate.now(clock));
        var found = logs.findBetween(period.from(), period.to(), limitOf(query.limit()));

        return Result.success(TrainingMapper.toLogs(found));
    }

    private static int limitOf(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAX_LIMIT);
    }
}
