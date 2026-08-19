package atlas.application.routines.commands.clearday;

import atlas.domain.routines.RoutineId;
import java.time.LocalDate;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record ClearDayCommand(RoutineId routineId, LocalDate day) implements Command<Result<Void>> {}
