package atlas.application.training.queries.listworkoutlogs;

import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record ListWorkoutLogsQuery(LocalDate from, LocalDate to, Integer limit)
    implements Query<Result<List<WorkoutLogDto>>> {}
