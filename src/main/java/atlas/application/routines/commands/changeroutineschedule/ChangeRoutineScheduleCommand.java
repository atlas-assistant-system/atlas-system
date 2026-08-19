package atlas.application.routines.commands.changeroutineschedule;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

public record ChangeRoutineScheduleCommand(
    RoutineId routineId,
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) implements Command<Result<RoutineDto>> {}
