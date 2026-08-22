package atlas.application.training.commands.setworkoutplan;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.commands.PlannedLineInput;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;
import java.util.List;

public record SetWorkoutPlanCommand(WorkoutId workoutId, List<PlannedLineInput> lines)
    implements Command<Result<WorkoutDto>> {}
