package atlas.application.routines.queries.gettoday;

import atlas.application.routines.dto.TodayRoutineDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.routines.Routine;
import atlas.domain.routines.services.RoutineProgress;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class GetTodayQueryHandler implements QueryHandler<GetTodayQuery, Result<List<TodayRoutineDto>>> {

    private final RoutineReadModel routines;
    private final RoutineProgress progress;
    private final Clock clock;

    public GetTodayQueryHandler(RoutineReadModel routines, RoutineProgress progress, Clock clock) {
        this.routines = routines;
        this.progress = progress;
        this.clock = clock;
    }

    @Override
    public Result<List<TodayRoutineDto>> handle(GetTodayQuery query) {
        var today = LocalDate.now(clock);
        var due = routines.findAll(false).stream()
            .filter(routine -> routine.occursOn(today))
            .sorted(Comparator.comparing(routine -> routine.name().value()))
            .toList();

        if (due.isEmpty()) {
            return Result.success(List.of());
        }

        var from = due.stream()
            .map(routine -> routine.schedule().windowFor(today).start())
            .min(Comparator.naturalOrder())
            .orElseThrow();
        var entries = routines.findEntries(due.stream().map(Routine::id).toList(), from, today.plusDays(1));

        return Result.success(due.stream().map(routine -> toDto(routine, entries, today)).toList());
    }

    private TodayRoutineDto toDto(Routine routine, List<atlas.domain.routines.RoutineEntry> entries,
        LocalDate today) {
        var window = routine.schedule().windowFor(today);

        return new TodayRoutineDto(
            RoutineMapper.toSummary(routine),
            RoutineMapper.toDto(routine.id(), progress.progressIn(window, routine, entries), today));
    }
}
