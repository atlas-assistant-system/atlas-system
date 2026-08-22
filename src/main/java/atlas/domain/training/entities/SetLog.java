package atlas.domain.training.entities;

import atlas.domain.sharedkernel.ddd.Entity;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.vos.Effort;
import java.util.Optional;

public final class SetLog extends Entity<SetLogId> {

    private final ExerciseId exerciseId;
    private final Optional<Effort> planned;

    private int position;
    private Optional<Effort> actual;

    public SetLog(
        SetLogId id,
        ExerciseId exerciseId,
        int position,
        Optional<Effort> planned,
        Optional<Effort> actual) {
        super(ObjectGuard.notNull(id, "id"));
        this.exerciseId = ObjectGuard.notNull(exerciseId, "exerciseId");
        this.position = position;
        this.planned = ObjectGuard.notNull(planned, "planned");
        this.actual = ObjectGuard.notNull(actual, "actual");
    }

    /** Solo lo llama WorkoutLog: los cambios de una serie pasan por la raíz. */
    public void record(Effort effort) {
        this.actual = Optional.of(effort);
    }

    /** Solo lo llama WorkoutLog al cerrar el hueco que deja una serie borrada. */
    public void moveTo(int newPosition) {
        this.position = newPosition;
    }

    public ExerciseId exerciseId() {
        return exerciseId;
    }

    public int position() {
        return position;
    }

    public Optional<Effort> planned() {
        return planned;
    }

    public Optional<Effort> actual() {
        return actual;
    }
}
