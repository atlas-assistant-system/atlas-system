package atlas.application.routines.commands.unarchiveroutine;

import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record UnarchiveRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
