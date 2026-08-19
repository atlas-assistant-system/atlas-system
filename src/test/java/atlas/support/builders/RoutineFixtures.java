package atlas.support.builders;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public final class RoutineFixtures {

    public static final RoutineId ROUTINE_ID = RoutineId.of(1);
    public static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");

    private RoutineFixtures() {}

    public static Routine routine(RecurrencePeriod period, int target) {
        return routine(period, target, Schedule.over(period).value());
    }

    public static Routine daily(int target, DayOfWeek... activeDays) {
        return routine(RecurrencePeriod.DAY, target, Schedule.daily(Set.of(activeDays)).value());
    }

    public static Routine monthlyOnDays(int target, Integer... daysOfMonth) {
        return routine(
            RecurrencePeriod.MONTH, target,
            Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(daysOfMonth)).value());
    }

    public static Routine routine(RecurrencePeriod period, int target, Schedule schedule) {
        return routine(ROUTINE_ID, "Rutina de prueba", target, schedule);
    }

    public static Routine routine(RoutineId id, String name, int target, Schedule schedule) {
        return Routine.rehydrate(
            id,
            RoutineName.create(name).value(),
            null,
            Target.create(BigDecimal.valueOf(target)).value(),
            schedule,
            false);
    }

    public static Routine archived(RecurrencePeriod period, int target) {
        var routine = routine(period, target);
        routine.archive(NOW);
        routine.clearEvents();

        return routine;
    }

    public static RoutineEntry entry(LocalDate day, int amount) {
        return entry(ROUTINE_ID, day, amount);
    }

    public static RoutineEntry entry(RoutineId routineId, LocalDate day, int amount) {
        return RoutineEntry.rehydrate(
            RoutineEntryId.of(UUID.randomUUID()), routineId, day, BigDecimal.valueOf(amount));
    }
}
