package atlas.domain.routines;

import java.util.UUID;
import sharedkernel.domain.ddd.SingleValueObject;

public record RoutineEntryId(UUID value) implements SingleValueObject<UUID> {

    public static RoutineEntryId of(UUID value) {
        return new RoutineEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
