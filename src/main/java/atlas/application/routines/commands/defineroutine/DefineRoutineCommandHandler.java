package atlas.application.routines.commands.defineroutine;

import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.mappers.RoutineMapper;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.Routine;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.domain.routines.vos.Unit;
import java.time.Clock;
import java.time.Instant;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class DefineRoutineCommandHandler implements CommandHandler<DefineRoutineCommand, Result<RoutineDto>> {

    private final RoutineUnitOfWork unitOfWork;
    private final Clock clock;

    public DefineRoutineCommandHandler(RoutineUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<RoutineDto> handle(DefineRoutineCommand command) {
        var nameResult = RoutineName.create(command.name());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var descriptionResult = RoutineDescription.create(command.description());
        if (descriptionResult.isFailure()) {
            return Result.failure(descriptionResult.error());
        }

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

        return unitOfWork.execute(() -> define(
            nameResult.value(),
            descriptionResult.value().orElse(null),
            targetResult.value(),
            scheduleResult.value(),
            occurredOn));
    }

    private Result<RoutineDto> define(
        RoutineName name,
        RoutineDescription description,
        Target target,
        Schedule schedule,
        Instant occurredOn) {

        var routines = unitOfWork.routines();
        var routineResult = Routine.define(routines.nextId(), name, description, target, schedule, occurredOn);
        if (routineResult.isFailure()) {
            return Result.failure(routineResult.error());
        }

        var routine = routineResult.value();
        routines.create(routine);

        return Result.success(RoutineMapper.toDto(routine));
    }
}
