package atlas.domain.training;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.events.ExerciseArchivedEvent;
import atlas.domain.training.events.ExerciseDefinedEvent;
import atlas.domain.training.events.ExerciseRenamedEvent;
import atlas.domain.training.events.ExerciseUnarchivedEvent;
import atlas.domain.training.vos.ExerciseName;
import java.time.Instant;

public final class Exercise extends AggregateRoot<ExerciseId> {

    private final Metric metric;

    private ExerciseName name;
    private boolean archived;

    private Exercise(ExerciseId id, ExerciseName name, Metric metric, boolean archived) {
        super(ObjectGuard.notNull(id, "id"));
        this.name = ObjectGuard.notNull(name, "name");
        this.metric = ObjectGuard.notNull(metric, "metric");
        this.archived = archived;
    }

    public static Result<Exercise> define(
        ExerciseId id, ExerciseName name, Metric metric, Instant now) {

        var exercise = new Exercise(id, name, metric, false);
        exercise.registerEvent(new ExerciseDefinedEvent(id, now));

        return Result.success(exercise);
    }

    public static Exercise rehydrate(
        ExerciseId id, ExerciseName name, Metric metric, boolean archived) {

        return new Exercise(id, name, metric, archived);
    }

    public Result<Void> rename(ExerciseName newName, Instant now) {
        if (archived) {
            return Result.failure(ExerciseErrors.ALREADY_ARCHIVED);
        }

        this.name = ObjectGuard.notNull(newName, "newName");
        registerEvent(new ExerciseRenamedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> archive(Instant now) {
        if (archived) {
            return Result.failure(ExerciseErrors.ALREADY_ARCHIVED);
        }

        this.archived = true;
        registerEvent(new ExerciseArchivedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> unarchive(Instant now) {
        if (!archived) {
            return Result.failure(ExerciseErrors.NOT_ARCHIVED);
        }

        this.archived = false;
        registerEvent(new ExerciseUnarchivedEvent(id(), now));

        return Result.success();
    }

    public ExerciseName name() {
        return name;
    }

    public Metric metric() {
        return metric;
    }

    public boolean isArchived() {
        return archived;
    }
}
