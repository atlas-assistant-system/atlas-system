package atlas.application.training.commands.recordset;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.commands.EffortInput;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.SetLogId;

public record RecordSetCommand(WorkoutLogId logId, SetLogId setId, EffortInput actual)
    implements Command<Result<WorkoutLogDto>> {}
