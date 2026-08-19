package atlas.application.routines.commands.archiveroutine;

import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record ArchiveRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
