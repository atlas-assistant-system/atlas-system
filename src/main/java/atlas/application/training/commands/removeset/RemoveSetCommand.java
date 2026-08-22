package atlas.application.training.commands.removeset;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.SetLogId;

public record RemoveSetCommand(WorkoutLogId logId, SetLogId setId) implements Command<Result<Void>> {}
