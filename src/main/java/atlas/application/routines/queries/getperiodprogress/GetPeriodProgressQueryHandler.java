package atlas.application.routines.queries.getperiodprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.services.RoutineProgress;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class GetPeriodProgressQueryHandler
    implements QueryHandler<GetPeriodProgressQuery, Result<PeriodProgressDto>> {

    private final RoutineReadModel routines;
    private final RoutineProgress progress;
    private final Clock clock;

    public GetPeriodProgressQueryHandler(RoutineReadModel routines, RoutineProgress progress, Clock clock) {
        this.routines = routines;
        this.progress = progress;
        this.clock = clock;
    }

    @Override
    public Result<PeriodProgressDto> handle(GetPeriodProgressQuery query) {
        var found = routines.find(query.routineId());
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(query.routineId()));
        }

        var routine = found.get();
        var window = routine.schedule().windowFor(query.dayInPeriod());
        var entries = routines.findEntries(routine.id(), window.start(), window.end());

        return Result.success(RoutineMapper.toDto(
            routine.id(), progress.progressIn(window, routine, entries), LocalDate.now(clock)));
    }
}
