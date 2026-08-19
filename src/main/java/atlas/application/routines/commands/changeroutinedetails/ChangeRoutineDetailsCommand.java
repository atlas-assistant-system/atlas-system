package atlas.application.routines.commands.changeroutinedetails;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;

public record ChangeRoutineDetailsCommand(RoutineId routineId, String name, String description)
    implements Command<Result<RoutineDto>> {}
