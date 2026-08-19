package atlas.application.routines.commands.deleteroutine;

import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;

public final class DeleteRoutineCommandHandler implements CommandHandler<DeleteRoutineCommand, Result<Void>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteRoutineCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteRoutineCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> delete(command.routineId(), occurredOn));
    }

    private Result<Void> delete(RoutineId routineId, Instant occurredOn) {
        var routines = unitOfWork.routines();
        var found = routines.get(routineId);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(routineId));
        }

        var routine = found.get();
        var deleted = routine.delete(occurredOn);
        if (deleted.isFailure()) {
            return Result.failure(deleted.error());
        }

        unitOfWork.entries().deleteAllOf(routine.id());
        routines.delete(routine);

        return Result.success();
    }
}
