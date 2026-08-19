package atlas.application.routines.commands.clearday;

import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class ClearDayCommandHandler implements CommandHandler<ClearDayCommand, Result<Void>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public ClearDayCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ClearDayCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> clear(command.routineId(), command.day(), occurredOn));
    }

    private Result<Void> clear(RoutineId routineId, LocalDate day, Instant occurredOn) {
        var entries = unitOfWork.entries();
        var found = entries.find(routineId, day);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.entryNotFound(routineId, day));
        }

        var entry = found.get();
        var cleared = entry.clear(occurredOn);
        if (cleared.isFailure()) {
            return Result.failure(cleared.error());
        }

        entries.delete(entry);

        return Result.success();
    }
}
