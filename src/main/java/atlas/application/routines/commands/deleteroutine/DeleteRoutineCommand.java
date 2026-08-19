package atlas.application.routines.commands.deleteroutine;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
