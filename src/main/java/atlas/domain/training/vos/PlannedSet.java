package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.training.ExerciseId;


public record PlannedSet(ExerciseId exerciseId, Effort target) implements ValueObject {

    public PlannedSet {
        ObjectGuard.notNull(exerciseId, "exerciseId");
        ObjectGuard.notNull(target, "target");
    }
}
