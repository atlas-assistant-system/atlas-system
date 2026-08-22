package atlas.application.training.commands.unarchiveexercise;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;

public record UnarchiveExerciseCommand(ExerciseId exerciseId) implements Command<Result<Void>> {}
