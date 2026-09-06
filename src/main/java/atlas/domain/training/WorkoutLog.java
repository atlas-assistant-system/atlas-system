package atlas.domain.training;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.entities.SetLog;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.events.SetRecordedEvent;
import atlas.domain.training.events.SetRemovedEvent;
import atlas.domain.training.events.WorkoutLogDiscardedEvent;
import atlas.domain.training.events.WorkoutStartedEvent;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.PlannedSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


public final class WorkoutLog extends AggregateRoot<WorkoutLogId> {

    private final List<SetLog> sets = new ArrayList<>();
    private final Optional<WorkoutId> workoutId;
    private final LocalDate performedOn;
    private final Instant startedAt;

    private WorkoutLog(
        WorkoutLogId id,
        Optional<WorkoutId> workoutId,
        LocalDate performedOn,
        Instant startedAt,
        List<SetLog> sets) {
        super(ObjectGuard.notNull(id, "id"));
        this.workoutId = ObjectGuard.notNull(workoutId, "workoutId");
        this.performedOn = ObjectGuard.notNull(performedOn, "performedOn");
        this.startedAt = ObjectGuard.notNull(startedAt, "startedAt");
        this.sets.addAll(ObjectGuard.notNull(sets, "sets"));
    }

    
    public static Result<WorkoutLog> start(
        WorkoutLogId id,
        Optional<WorkoutId> workoutId,
        List<PlannedSet> plan,
        Supplier<SetLogId> setIds,
        LocalDate performedOn,
        LocalDate today,
        Instant now) {

        if (performedOn.isAfter(today)) {
            return Result.failure(WorkoutLogErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        }

        var pending = new ArrayList<SetLog>(plan.size());
        for (var position = 0; position < plan.size(); position++) {
            var planned = plan.get(position);
            pending.add(new SetLog(
                setIds.get(), planned.exerciseId(), position,
                Optional.of(planned.target()), Optional.empty()));
        }

        var log = new WorkoutLog(id, workoutId, performedOn, now, pending);
        log.registerEvent(new WorkoutStartedEvent(id, now));

        return Result.success(log);
    }

    public static WorkoutLog rehydrate(
        WorkoutLogId id,
        Optional<WorkoutId> workoutId,
        LocalDate performedOn,
        Instant startedAt,
        List<SetLog> sets) {

        return new WorkoutLog(id, workoutId, performedOn, startedAt, sets);
    }

    
    public Result<Void> recordSet(SetLogId setId, Effort actual, Instant now) {
        if (actual.isZero()) {
            return Result.failure(WorkoutLogErrors.SET_MEASURES_NOTHING);
        }

        var found = find(setId);
        if (found.isEmpty()) {
            return Result.failure(WorkoutLogErrors.SET_NOT_FOUND);
        }

        found.get().record(actual);
        registerEvent(new SetRecordedEvent(id(), now));

        return Result.success();
    }

    
    public Result<Void> addSet(SetLogId setId, ExerciseId exerciseId, Effort actual, Instant now) {
        if (actual.isZero()) {
            return Result.failure(WorkoutLogErrors.SET_MEASURES_NOTHING);
        }

        sets.add(new SetLog(setId, exerciseId, sets.size(), Optional.empty(), Optional.of(actual)));
        registerEvent(new SetRecordedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> removeSet(SetLogId setId, Instant now) {
        var found = find(setId);
        if (found.isEmpty()) {
            return Result.failure(WorkoutLogErrors.SET_NOT_FOUND);
        }

        sets.remove(found.get());
        renumber();
        registerEvent(new SetRemovedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> discard(Instant now) {
        registerEvent(new WorkoutLogDiscardedEvent(id(), now));

        return Result.success();
    }

    public List<SetLog> sets() {
        return List.copyOf(sets);
    }

    public Optional<WorkoutId> workoutId() {
        return workoutId;
    }

    public LocalDate performedOn() {
        return performedOn;
    }

    public Instant startedAt() {
        return startedAt;
    }

    private Optional<SetLog> find(SetLogId setId) {
        return sets.stream().filter(set -> set.id().equals(setId)).findFirst();
    }

    private void renumber() {
        for (var position = 0; position < sets.size(); position++) {
            sets.get(position).moveTo(position);
        }
    }
}
