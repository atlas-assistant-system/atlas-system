package atlas.application.routines.commands.logprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.domain.routines.RoutineId;
import java.math.BigDecimal;
import java.time.LocalDate;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record LogProgressCommand(RoutineId routineId, LocalDate day, BigDecimal amount)
    implements Command<Result<PeriodProgressDto>> {}
