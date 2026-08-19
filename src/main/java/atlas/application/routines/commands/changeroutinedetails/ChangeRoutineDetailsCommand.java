package atlas.application.routines.commands.changeroutinedetails;

import atlas.application.routines.dto.RoutineDto;
import atlas.domain.routines.RoutineId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record ChangeRoutineDetailsCommand(RoutineId routineId, String name, String description)
    implements Command<Result<RoutineDto>> {}
