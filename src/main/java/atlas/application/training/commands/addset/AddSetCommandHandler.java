package atlas.application.training.commands.addset;

import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.mappers.TrainingMapper;
import atlas.application.training.ports.IdGenerator;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutLogErrors;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.vos.Effort;
import java.time.Clock;

public final class AddSetCommandHandler
    implements CommandHandler<AddSetCommand, Result<WorkoutLogDto>> {

    private final TrainingUnitOfWork unitOfWork;
    private final Clock clock;
    private final IdGenerator ids;

    public AddSetCommandHandler(TrainingUnitOfWork unitOfWork, Clock clock, IdGenerator ids) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public Result<WorkoutLogDto> handle(AddSetCommand command) {
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
            var added = log.addSet(
                SetLogId.of(ids.next()), command.exerciseId(), effort.value(), now);
            if (added.isFailure()) {
                return Result.failure(added.error());
            }

            logs.update(log);

            return Result.success(TrainingMapper.toDto(log));
        });
    }
}
