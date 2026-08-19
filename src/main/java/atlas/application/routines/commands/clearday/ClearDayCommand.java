package atlas.application.routines.commands.clearday;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record ClearDayCommand(RoutineId routineId, LocalDate day) implements Command<Result<Void>> {}
