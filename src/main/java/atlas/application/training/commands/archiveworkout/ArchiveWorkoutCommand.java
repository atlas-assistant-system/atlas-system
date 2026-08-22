package atlas.application.training.commands.archiveworkout;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;

public record ArchiveWorkoutCommand(WorkoutId workoutId) implements Command<Result<Void>> {}
