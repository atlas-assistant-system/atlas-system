package atlas.domain.training.entities;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.UUID;

public record SetLogId(UUID value) implements SingleValueObject<UUID> {

    public SetLogId {
        ObjectGuard.notNull(value, "value");
    }

    public static SetLogId of(UUID value) {
        return new SetLogId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
