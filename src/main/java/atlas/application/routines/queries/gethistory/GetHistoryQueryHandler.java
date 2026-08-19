package atlas.application.routines.queries.gethistory;

import atlas.application.routines.dto.HistoryDayDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

public final class GetHistoryQueryHandler implements QueryHandler<GetHistoryQuery, Result<List<HistoryDayDto>>> {

    private final RoutineReadModel routines;

    public GetHistoryQueryHandler(RoutineReadModel routines) {
        this.routines = routines;
    }

    @Override
    public Result<List<HistoryDayDto>> handle(GetHistoryQuery query) {
        var found = routines.find(query.routineId());
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(query.routineId()));
        }
        if (query.to().isBefore(query.from())) {
            return Result.success(List.of());
        }

        var routine = found.get();
        var toExclusive = query.to().plusDays(1);
        var logged = new HashMap<LocalDate, BigDecimal>();

        for (var entry : routines.findEntries(routine.id(), query.from(), toExclusive)) {
            logged.merge(entry.day(), entry.amount(), BigDecimal::add);
        }

        var days = query
            .from()
            .datesUntil(toExclusive)
            .map(day -> new HistoryDayDto(day, logged.getOrDefault(day, BigDecimal.ZERO), routine.occursOn(day)))
            .toList();

        return Result.success(days);
    }
}
