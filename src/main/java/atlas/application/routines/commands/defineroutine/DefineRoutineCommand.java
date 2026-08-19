package atlas.application.routines.commands.defineroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

public record DefineRoutineCommand(
    String name,
    String description,
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) implements Command<Result<RoutineDto>> {}
