package atlas.application.training.commands.addset;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.commands.EffortInput;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.WorkoutLogId;

public record AddSetCommand(WorkoutLogId logId, ExerciseId exerciseId, EffortInput actual)
    implements Command<Result<WorkoutLogDto>> {}
