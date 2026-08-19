package atlas.application.routines.commands.changeroutineschedule;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.domain.routines.vos.Unit;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;

public final class ChangeRoutineScheduleCommandHandler
    implements CommandHandler<ChangeRoutineScheduleCommand, Result<RoutineDto>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public ChangeRoutineScheduleCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<RoutineDto> handle(ChangeRoutineScheduleCommand command) {
        var unitResult = Unit.create(command.unit());
        if (unitResult.isFailure()) {
            return Result.failure(unitResult.error());
        }

        var targetResult = Target.create(command.target(), unitResult.value().orElse(null));
        if (targetResult.isFailure()) {
            return Result.failure(targetResult.error());
        }

        var scheduleResult = Schedule.create(command.period(), command.activeDays(), command.daysOfMonth());
        if (scheduleResult.isFailure()) {
            return Result.failure(scheduleResult.error());
        }

        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> changeSchedule(
            command.routineId(), scheduleResult.value(), targetResult.value(), occurredOn));
    }

    private Result<RoutineDto> changeSchedule(
        RoutineId routineId,
        Schedule schedule,
        Target target,
        Instant occurredOn) {

        var routines = unitOfWork.routines();
        var found = routines.get(routineId);
        if (found.isEmpty()) {
            return Result.failure(RoutineErrors.notFound(routineId));
        }

        var routine = found.get();
        var changed = routine.changeSchedule(schedule, target, occurredOn);
        if (changed.isFailure()) {
            return Result.failure(changed.error());
        }

        routines.update(routine);

        return Result.success(RoutineMapper.toDto(routine));
    }
}
