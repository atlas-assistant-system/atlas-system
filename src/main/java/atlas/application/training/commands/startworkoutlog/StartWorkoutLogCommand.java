package atlas.application.training.commands.startworkoutlog;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutId;
import java.time.LocalDate;


public record StartWorkoutLogCommand(WorkoutId workoutId, LocalDate performedOn)
    implements Command<Result<WorkoutLogDto>> {}
