package atlas.domain.training;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.events.WorkoutArchivedEvent;
import atlas.domain.training.events.WorkoutDefinedEvent;
import atlas.domain.training.events.WorkoutPlanChangedEvent;
import atlas.domain.training.events.WorkoutRenamedEvent;
import atlas.domain.training.events.WorkoutScheduledEvent;
import atlas.domain.training.vos.PlannedLine;
import atlas.domain.training.vos.PlannedSet;
import atlas.domain.training.vos.WorkoutName;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Workout extends AggregateRoot<WorkoutId> {

    private final List<PlannedExercise> plan = new ArrayList<>();

    private final EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

    private WorkoutName name;
    private boolean archived;

    private Workout(
        WorkoutId id,
        WorkoutName name,
        List<PlannedExercise> plan,
        Set<DayOfWeek> days,
        boolean archived) {

        super(ObjectGuard.notNull(id, "id"));
        this.name = ObjectGuard.notNull(name, "name");
        this.plan.addAll(ObjectGuard.notNull(plan, "plan"));
        this.days.addAll(ObjectGuard.notNull(days, "days"));
        this.archived = archived;
    }

    public static Result<Workout> define(WorkoutId id, WorkoutName name, Instant now) {
        var workout = new Workout(id, name, List.of(), Set.of(), false);
        workout.registerEvent(new WorkoutDefinedEvent(id, now));

        return Result.success(workout);
    }

    public static Workout rehydrate(
        WorkoutId id,
        WorkoutName name,
        List<PlannedExercise> plan,
        Set<DayOfWeek> days,
        boolean archived) {

        return new Workout(id, name, plan, days, archived);
    }

    public Result<Void> rename(WorkoutName newName, Instant now) {
        if (archived) {
            return Result.failure(WorkoutErrors.ALREADY_ARCHIVED);
        }

        this.name = ObjectGuard.notNull(newName, "newName");
        registerEvent(new WorkoutRenamedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> setPlan(List<PlannedLine> lines, Instant now) {
        if (archived) {
            return Result.failure(WorkoutErrors.ALREADY_ARCHIVED);
        }

        ObjectGuard.notNull(lines, "lines");

        var replacement = new ArrayList<PlannedExercise>(lines.size());
        for (var position = 0; position < lines.size(); position++) {
            var line = lines.get(position);
            replacement.add(new PlannedExercise(
                line.id(), line.exerciseId(), position, line.sets(), line.target()));
        }

        plan.clear();
        plan.addAll(replacement);
        registerEvent(new WorkoutPlanChangedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> scheduleOn(Set<DayOfWeek> weekdays, Instant now) {
        if (archived) {
            return Result.failure(WorkoutErrors.ALREADY_ARCHIVED);
        }

        ObjectGuard.notNull(weekdays, "weekdays");

        days.clear();
        days.addAll(weekdays);
        registerEvent(new WorkoutScheduledEvent(id(), now));

        return Result.success();
    }

    public Result<Void> archive(Instant now) {
        if (archived) {
            return Result.failure(WorkoutErrors.ALREADY_ARCHIVED);
        }

        this.archived = true;
        registerEvent(new WorkoutArchivedEvent(id(), now));

        return Result.success();
    }

    public List<PlannedSet> expand() {
        var sets = new ArrayList<PlannedSet>();

        for (var line : plan) {
            for (var repetition = 0; repetition < line.sets().value(); repetition++) {
                sets.add(new PlannedSet(line.exerciseId(), line.target()));
            }
        }

        return List.copyOf(sets);
    }

    public List<PlannedExercise> plan() {
        return List.copyOf(plan);
    }

    public Set<DayOfWeek> days() {
        return Collections.unmodifiableSet(EnumSet.copyOf(days));
    }

    public boolean isScheduledOn(DayOfWeek day) {
        return days.contains(day);
    }

    public WorkoutName name() {
        return name;
    }

    public boolean isArchived() {
        return archived;
    }
}
