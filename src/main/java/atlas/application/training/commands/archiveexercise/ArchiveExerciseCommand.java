package atlas.application.training.commands.archiveexercise;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;

public record ArchiveExerciseCommand(ExerciseId exerciseId) implements Command<Result<Void>> {}
