package atlas.application.training.commands.renameexercise;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.ExerciseDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;

public record RenameExerciseCommand(ExerciseId exerciseId, String name)
    implements Command<Result<ExerciseDto>> {}
