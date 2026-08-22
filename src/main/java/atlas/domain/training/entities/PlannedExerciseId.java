package atlas.domain.training.entities;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.UUID;

public record PlannedExerciseId(UUID value) implements SingleValueObject<UUID> {

    public PlannedExerciseId {
        ObjectGuard.notNull(value, "value");
    }

    public static PlannedExerciseId of(UUID value) {
        return new PlannedExerciseId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
