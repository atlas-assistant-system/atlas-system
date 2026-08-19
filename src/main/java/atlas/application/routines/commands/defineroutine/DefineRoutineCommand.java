package atlas.application.routines.commands.defineroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.domain.routines.enums.RecurrencePeriod;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record DefineRoutineCommand(
    String name,
    String description,
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) implements Command<Result<RoutineDto>> {}
