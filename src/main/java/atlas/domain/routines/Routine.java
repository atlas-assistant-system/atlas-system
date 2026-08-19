package atlas.domain.routines;

import atlas.domain.routines.events.RoutineArchivedEvent;
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.events.RoutineDeletedEvent;
import atlas.domain.routines.events.RoutineDetailsChangedEvent;
import atlas.domain.routines.events.RoutineScheduleChangedEvent;
import atlas.domain.routines.events.RoutineUnarchivedEvent;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public final class Routine extends AggregateRoot<RoutineId> {

    private RoutineName name;
    private RoutineDescription description;
    private Target target;
    private Schedule schedule;
    private boolean archived;

    private Routine(
        RoutineId id,
        RoutineName name,
        RoutineDescription description,
        Target target,
        Schedule schedule,
        boolean archived) {
        super(ObjectGuard.notNull(id, "id"));
        this.name = ObjectGuard.notNull(name, "name");
        this.description = description;
        this.target = ObjectGuard.notNull(target, "target");
        this.schedule = ObjectGuard.notNull(schedule, "schedule");
        this.archived = archived;
    }

    public static Result<Routine> define(
        RoutineId id,
        RoutineName name,
        RoutineDescription description,
        Target target,
        Schedule schedule,
        Instant occurredOn) {

        var routine = new Routine(id, name, description, target, schedule, false);
        routine.registerEvent(new RoutineDefinedEvent(id, name, occurredOn));

        return Result.success(routine);
    }

    public static Routine rehydrate(
        RoutineId id,
        RoutineName name,
        RoutineDescription description,
        Target target,
        Schedule schedule,
        boolean archived) {

        return new Routine(id, name, description, target, schedule, archived);
    }

    public Result<Void> changeDetails(RoutineName newName, RoutineDescription newDescription, Instant occurredOn) {
        this.name = ObjectGuard.notNull(newName, "newName");
        this.description = newDescription;
        registerEvent(new RoutineDetailsChangedEvent(id(), occurredOn));

        return Result.success();
    }

    public Result<Void> changeSchedule(Schedule newSchedule, Target newTarget, Instant occurredOn) {
        if (archived) {
            return Result.failure(RoutineErrors.ROUTINE_IS_ARCHIVED);
        }

        this.schedule = ObjectGuard.notNull(newSchedule, "newSchedule");
        this.target = ObjectGuard.notNull(newTarget, "newTarget");
        registerEvent(new RoutineScheduleChangedEvent(id(), newSchedule, occurredOn));

        return Result.success();
    }

    public Result<Void> archive(Instant occurredOn) {
        if (archived) {
            return Result.failure(RoutineErrors.ROUTINE_ALREADY_ARCHIVED);
        }

        this.archived = true;
        registerEvent(new RoutineArchivedEvent(id(), occurredOn));

        return Result.success();
    }

    public Result<Void> unarchive(Instant occurredOn) {
        if (!archived) {
            return Result.failure(RoutineErrors.ROUTINE_NOT_ARCHIVED);
        }

        this.archived = false;
        registerEvent(new RoutineUnarchivedEvent(id(), occurredOn));

        return Result.success();
    }

    public Result<Void> delete(Instant occurredOn) {
        registerEvent(new RoutineDeletedEvent(id(), occurredOn));

        return Result.success();
    }

    public Result<Void> checkCanLogOn(LocalDate day) {
        if (archived) {
            return Result.failure(RoutineErrors.ROUTINE_IS_ARCHIVED);
        }

        if (!occursOn(day)) {
            return Result.failure(RoutineErrors.DAY_NOT_SCHEDULED);
        }

        return Result.success();
    }

    public boolean occursOn(LocalDate day) {
        return schedule.includes(day);
    }

    public RoutineName name() {
        return name;
    }

    public Optional<RoutineDescription> description() {
        return Optional.ofNullable(description);
    }

    public Target target() {
        return target;
    }

    public Schedule schedule() {
        return schedule;
    }

    public boolean isArchived() {
        return archived;
    }
}
