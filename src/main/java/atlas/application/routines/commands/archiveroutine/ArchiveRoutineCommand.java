package atlas.application.routines.commands.archiveroutine;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record ArchiveRoutineCommand(RoutineId routineId) implements Command<Result<Void>> {}
