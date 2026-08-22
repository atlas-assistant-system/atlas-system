package atlas.domain.training.entities;

import atlas.domain.sharedkernel.ddd.Entity;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.SetCount;

public final class PlannedExercise extends Entity<PlannedExerciseId> {

    private final ExerciseId exerciseId;
    private final int position;
    private final SetCount sets;
    private final Effort target;

    public PlannedExercise(
        PlannedExerciseId id, ExerciseId exerciseId, int position, SetCount sets, Effort target) {
        super(ObjectGuard.notNull(id, "id"));
        this.exerciseId = ObjectGuard.notNull(exerciseId, "exerciseId");
        this.position = position;
        this.sets = ObjectGuard.notNull(sets, "sets");
        this.target = ObjectGuard.notNull(target, "target");
    }

    public ExerciseId exerciseId() {
        return exerciseId;
    }

    public int position() {
        return position;
    }

    public SetCount sets() {
        return sets;
    }

    public Effort target() {
        return target;
    }
}
