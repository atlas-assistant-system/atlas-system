package atlas.application.training.commands.recordset;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogErrors;
import atlas.domain.training.vos.Effort;
import java.time.Clock;

public final class RecordSetCommandHandler
    implements CommandHandler<RecordSetCommand, Result<WorkoutLogDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;

    public RecordSetCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<WorkoutLogDto> handle(RecordSetCommand command) {
        var actual = command.actual();
        var effort = Effort.ofKilograms(
            actual.load(), actual.reps(), actual.seconds(), actual.meters());
        if (effort.isFailure()) {
            return Result.failure(effort.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var logs = unitOfWork.logs();
            var found = logs.get(command.logId());
            if (found.isEmpty()) {
                return Result.failure(WorkoutLogErrors.notFound(command.logId()));
            }

            var log = found.get();
            var recorded = log.recordSet(command.setId(), effort.value(), now);
            if (recorded.isFailure()) {
                return Result.failure(recorded.error());
            }

            logs.update(log);

            return Result.success(TrainingMapper.toDto(log));
        });
    }
}
