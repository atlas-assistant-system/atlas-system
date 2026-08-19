package atlas.application.routines.commands.unarchiveroutine;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record UnarchiveRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
