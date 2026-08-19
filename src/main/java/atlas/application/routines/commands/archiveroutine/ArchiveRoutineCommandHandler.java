package atlas.application.routines.commands.archiveroutine;

import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;

public final class ArchiveRoutineCommandHandler implements CommandHandler<ArchiveRoutineCommand, Result<Void>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public ArchiveRoutineCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ArchiveRoutineCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> archive(command.routineId(), occurredOn));
    }

    private Result<Void> archive(RoutineId routineId, Instant occurredOn) {
        var routines = unitOfWork.routines();
        var found = routines.get(routineId);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(routineId));
        }

        var routine = found.get();
        var archived = routine.archive(occurredOn);
        if (archived.isFailure()) {
            return Result.failure(archived.error());
        }

        routines.update(routine);

        return Result.success();
    }
}
