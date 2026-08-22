package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.entities.PlannedExerciseId;

/** Una línea de la plantilla tal y como entra por {@code Workout.setPlan(...)}. */
public record PlannedLine(
    PlannedExerciseId id, ExerciseId exerciseId, SetCount sets, Effort target) implements ValueObject {

    public PlannedLine {
        ObjectGuard.notNull(id, "id");
        ObjectGuard.notNull(exerciseId, "exerciseId");
        ObjectGuard.notNull(sets, "sets");
        ObjectGuard.notNull(target, "target");
    }
}
