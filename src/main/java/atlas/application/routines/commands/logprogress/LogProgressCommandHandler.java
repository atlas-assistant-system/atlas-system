package atlas.application.routines.commands.logprogress;

import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineEntryIdGenerator;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.services.RoutineProgress;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class LogProgressCommandHandler implements CommandHandler<LogProgressCommand, Result<PeriodProgressDto>> {

    private final RoutineUnitOfWork unitOfWork;
    private final RoutineProgress progress;
    private final RoutineEntryIdGenerator entryIds;
    private final Clock clock;

    public LogProgressCommandHandler(
        RoutineUnitOfWork unitOfWork,
        RoutineProgress progress,
        RoutineEntryIdGenerator entryIds,
        Clock clock) {
        this.unitOfWork = unitOfWork;
        this.progress = progress;
        this.entryIds = entryIds;
        this.clock = clock;
    }

    @Override
    public Result<PeriodProgressDto> handle(LogProgressCommand command) {
        var occurredOn = clock.instant();
        var today = LocalDate.now(clock);

        return unitOfWork.execute(() -> log(command, today, occurredOn));
    }

    private Result<PeriodProgressDto> log(LogProgressCommand command, LocalDate today, Instant occurredOn) {
        var found = unitOfWork.routines().get(command.routineId());
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(command.routineId()));
        }

        var routine = found.get();
        var allowed = routine.checkCanLogOn(command.day());
        if (allowed.isFailure()) {
            return Result.failure(allowed.error());
        }

        var entries = unitOfWork.entries();
        var existing = entries.find(routine.id(), command.day());

        if (existing.isPresent()) {
            var entry = existing.get();
            var added = entry.add(command.amount(), occurredOn);
            if (added.isFailure()) {
                return Result.failure(added.error());
            }

            entries.update(entry);
        } else {
            var logged =
                RoutineEntry.log(entryIds.next(), routine.id(), command.day(), command.amount(), occurredOn);
            if (logged.isFailure()) {
                return Result.failure(logged.error());
            }

            entries.create(logged.value());
        }

        return Result.success(periodProgress(routine, command.day(), today));
    }

    private PeriodProgressDto periodProgress(Routine routine, LocalDate day, LocalDate today) {
        var window = routine.schedule().windowFor(day);
        var inWindow = unitOfWork.entries().findInWindow(routine.id(), window.start(), window.end());

        return RoutineMapper.toDto(routine.id(), progress.progressIn(window, routine, inWindow), today);
    }
}
