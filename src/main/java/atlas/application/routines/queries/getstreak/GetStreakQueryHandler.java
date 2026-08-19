package atlas.application.routines.queries.getstreak;

import atlas.application.routines.dto.StreakDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.services.RoutineProgress;
import java.time.Clock;
import java.time.LocalDate;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.results.Result;

public final class GetStreakQueryHandler implements QueryHandler<GetStreakQuery, Result<StreakDto>> {

    private final RoutineReadModel routines;
    private final RoutineProgress progress;
    private final Clock clock;

    public GetStreakQueryHandler(RoutineReadModel routines, RoutineProgress progress, Clock clock) {
        this.routines = routines;
        this.progress = progress;
        this.clock = clock;
    }

    @Override
    public Result<StreakDto> handle(GetStreakQuery query) {
        var found = routines.find(query.routineId());
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(query.routineId()));
        }

        var routine = found.get();
        var entries = routines.findAllEntries(routine.id());
        var streak = progress.streakAt(LocalDate.now(clock), routine, entries);

        return Result.success(RoutineMapper.toDto(routine.id(), streak));
    }
}
