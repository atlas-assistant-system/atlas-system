package atlas.application.routines.queries.getcompliancestats;

import atlas.application.routines.dto.ComplianceStatsDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.Routine;
import atlas.domain.routines.services.RoutineProgress;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.results.Result;

public final class GetComplianceStatsQueryHandler
    implements QueryHandler<GetComplianceStatsQuery, Result<List<ComplianceStatsDto>>> {

    private final RoutineReadModel routines;
    private final RoutineProgress progress;
    private final Clock clock;

    public GetComplianceStatsQueryHandler(RoutineReadModel routines, RoutineProgress progress, Clock clock) {
        this.routines = routines;
        this.progress = progress;
        this.clock = clock;
    }

    @Override
    public Result<List<ComplianceStatsDto>> handle(GetComplianceStatsQuery query) {
        if (query.to().isBefore(query.from())) {
            return Result.success(List.of());
        }

        var today = LocalDate.now(clock);
        var toExclusive = query.to().plusDays(1);
        var stats = routines.findAll(query.includeArchived()).stream()
            .map(routine -> statsOf(routine, query.from(), toExclusive, today))
            .toList();

        return Result.success(stats);
    }

    private ComplianceStatsDto statsOf(Routine routine, LocalDate from, LocalDate toExclusive, LocalDate today) {
        var period = routine.schedule().period();
        var entries = routines.findEntries(routine.id(), period.startOf(from), toExclusive);
        var closed = 0;
        var met = 0;

        for (var start = period.startOf(from); start.isBefore(toExclusive); start = period.next(start)) {
            var window = period.windowFor(start);

            if (!window.isClosedOn(today) || !routine.schedule().occursIn(window)) {
                continue;
            }

            closed++;
            if (progress.progressIn(window, routine, entries).isMet()) {
                met++;
            }
        }

        return new ComplianceStatsDto(
            routine.id().toString(),
            routine.name().value(),
            closed,
            met,
            closed == 0 ? 0d : (double) met / closed);
    }
}
