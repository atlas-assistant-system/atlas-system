package atlas.application.training.commands.defineworkout;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;

public record DefineWorkoutCommand(String name) implements Command<Result<WorkoutDto>> {}
