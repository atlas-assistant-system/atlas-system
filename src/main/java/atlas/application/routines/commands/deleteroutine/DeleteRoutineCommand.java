package atlas.application.routines.commands.deleteroutine;

import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record DeleteRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
