package atlas.application.routines.commands.changeroutineschedule;

import atlas.application.routines.dto.RoutineDto;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record ChangeRoutineScheduleCommand(
    RoutineId routineId,
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) implements Command<Result<RoutineDto>> {}
