package atlas.application.training.commands.scheduleworkout;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.WorkoutDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;
import java.time.DayOfWeek;
import java.util.Set;

public record ScheduleWorkoutCommand(WorkoutId workoutId, Set<DayOfWeek> days)
    implements Command<Result<WorkoutDto>> {}
