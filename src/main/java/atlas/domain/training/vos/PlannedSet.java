package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.training.ExerciseId;

/** Una serie del guion, ya desplegada: sale de {@code Workout.expand()}. */
public record PlannedSet(ExerciseId exerciseId, Effort target) implements ValueObject {

    public PlannedSet {
        ObjectGuard.notNull(exerciseId, "exerciseId");
        ObjectGuard.notNull(target, "target");
    }
}
