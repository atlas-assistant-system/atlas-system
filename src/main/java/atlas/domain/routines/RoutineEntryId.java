package atlas.domain.routines;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import java.util.UUID;

public record RoutineEntryId(UUID value) implements SingleValueObject<UUID> {

    public static RoutineEntryId of(UUID value) {
        return new RoutineEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
