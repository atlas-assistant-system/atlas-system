package atlas.application.routines.commands.changeroutinedetails;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import java.time.Clock;
import java.time.Instant;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class ChangeRoutineDetailsCommandHandler
    implements CommandHandler<ChangeRoutineDetailsCommand, Result<RoutineDto>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public ChangeRoutineDetailsCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<RoutineDto> handle(ChangeRoutineDetailsCommand command) {
        var nameResult = RoutineName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var descriptionResult = RoutineDescription.create(command.description());
        if (descriptionResult.isFailure()) {
            return Result.failure(descriptionResult.error());
        }

        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> changeDetails(
            command.routineId(), nameResult.value(), descriptionResult.value().orElse(null), occurredOn));
    }

    private Result<RoutineDto> changeDetails(
        RoutineId routineId,
        RoutineName name,
        RoutineDescription description,
        Instant occurredOn) {

        var routines = unitOfWork.routines();
        var found = routines.get(routineId);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(routineId));
        }

        var routine = found.get();
        var changed = routine.changeDetails(name, description, occurredOn);
        if (changed.isFailure()) {
            return Result.failure(changed.error());
        }

        routines.update(routine);

        return Result.success(RoutineMapper.toDto(routine));
    }
}
