package atlas.domain.routines.services;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.vos.PeriodProgress;
import atlas.domain.routines.vos.PeriodWindow;
import atlas.domain.routines.vos.Streak;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoutineProgress {

    public PeriodProgress progressIn(PeriodWindow window, Routine routine, List<RoutineEntry> entries) {
        var logged = entries.stream()
            .filter(entry -> entry.routineId().equals(routine.id()) && window.contains(entry.day()))
            .map(RoutineEntry::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PeriodProgress(window, logged, routine.target());
    }

    public Streak streakAt(LocalDate today, Routine routine, List<RoutineEntry> entries) {
        var own = entries.stream().filter(entry -> entry.routineId().equals(routine.id())).toList();
        if (own.isEmpty()) {
            return Streak.NONE;
        }

        var schedule = routine.schedule();
        var period = schedule.period();
        var currentPeriod = period.startOf(today);
        var firstDay = own.stream().map(RoutineEntry::day).min(Comparator.naturalOrder()).orElseThrow();
        var firstPeriod = period.startOf(firstDay);

        if (firstPeriod.isAfter(currentPeriod)) {
            return Streak.NONE;
        }

        var logged = totalsByPeriod(own, routine);
        var best = 0;
        var run = 0;

        for (var start = firstPeriod; !start.isAfter(currentPeriod); start = period.next(start)) {
            if (!schedule.occursIn(period.windowFor(start))) {
                continue;
            }

            if (routine.target().isMetBy(logged.getOrDefault(start, BigDecimal.ZERO))) {
                run++;
                best = Math.max(best, run);
            } else if (!start.equals(currentPeriod)) {
                run = 0;
            }
        }

        return new Streak(run, best);
    }

    private static Map<LocalDate, BigDecimal> totalsByPeriod(List<RoutineEntry> entries, Routine routine) {
        var totals = new HashMap<LocalDate, BigDecimal>();

        for (var entry : entries) {
            totals.merge(routine.schedule().windowFor(entry.day()).start(), entry.amount(), BigDecimal::add);
        }

        return totals;
    }
}
