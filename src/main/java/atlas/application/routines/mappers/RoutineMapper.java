package atlas.application.routines.mappers;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.dto.RoutineSummaryDto;
import atlas.application.routines.dto.StreakDto;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.PeriodProgress;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.Streak;
import atlas.domain.routines.vos.Unit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class RoutineMapper {

    private RoutineMapper() {}

    public static RoutineDto toDto(Routine routine) {
        var schedule = routine.schedule();

        return new RoutineDto(
            routine.id().toString(),
            routine.name().value(),
            routine.description().map(RoutineDescription::value).orElse(null),
            routine.target().amount(),
            routine.target().unit().map(Unit::value).orElse(null),
            schedule.period().name(),
            schedule.activeDays().stream().sorted().map(DayOfWeek::name).toList(),
            schedule.daysOfMonth().stream().sorted().toList(),
            routine.isArchived());
    }

    public static RoutineSummaryDto toSummary(Routine routine) {
        return new RoutineSummaryDto(
            routine.id().toString(),
            routine.name().value(),
            routine.target().amount(),
            routine.target().unit().map(Unit::value).orElse(null),
            routine.schedule().period().name(),
            routine.isArchived());
    }

    public static List<RoutineSummaryDto> toSummaries(List<Routine> routines) {
        return routines.stream().sorted(Comparator.comparing(r -> r.name().value())).map(RoutineMapper::toSummary)
            .toList();
    }

    public static PeriodProgressDto toDto(RoutineId routineId, PeriodProgress progress, LocalDate today) {
        return new PeriodProgressDto(
            routineId.toString(),
            progress.window().start(),
            progress.window().end(),
            progress.logged(),
            progress.target().amount(),
            progress.target().unit().map(Unit::value).orElse(null),
            progress.isMet(),
            progress.isClosedOn(today),
            progress.isFailedOn(today));
    }

    public static StreakDto toDto(RoutineId routineId, Streak streak) {
        return new StreakDto(routineId.toString(), streak.current(), streak.best());
    }
}
