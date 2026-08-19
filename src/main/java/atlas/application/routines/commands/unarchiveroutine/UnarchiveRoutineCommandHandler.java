package atlas.application.routines.commands.unarchiveroutine;

import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import java.time.Clock;
import java.time.Instant;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class UnarchiveRoutineCommandHandler implements CommandHandler<UnarchiveRoutineCommand, Result<Void>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public UnarchiveRoutineCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(UnarchiveRoutineCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> unarchive(command.routineId(), occurredOn));
    }

    private Result<Void> unarchive(RoutineId routineId, Instant occurredOn) {
        var routines = unitOfWork.routines();
        var found = routines.get(routineId);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(routineId));
        }

        var routine = found.get();
        var unarchived = routine.unarchive(occurredOn);
        if (unarchived.isFailure()) {
            return Result.failure(unarchived.error());
        }

        routines.update(routine);

        return Result.success();
    }
}
