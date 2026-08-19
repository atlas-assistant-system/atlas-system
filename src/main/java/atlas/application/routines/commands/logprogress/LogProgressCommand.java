package atlas.application.routines.commands.logprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LogProgressCommand(RoutineId routineId, LocalDate day, BigDecimal amount)
    implements Command<Result<PeriodProgressDto>> {}
