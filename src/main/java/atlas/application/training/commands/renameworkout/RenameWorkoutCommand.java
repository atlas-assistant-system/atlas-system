package atlas.application.training.commands.renameworkout;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;

public record RenameWorkoutCommand(WorkoutId workoutId, String name)
    implements Command<Result<WorkoutDto>> {}
