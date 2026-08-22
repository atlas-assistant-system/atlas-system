package atlas.application.training.commands.discardworkoutlog;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogId;

public record DiscardWorkoutLogCommand(WorkoutLogId logId) implements Command<Result<Void>> {}
