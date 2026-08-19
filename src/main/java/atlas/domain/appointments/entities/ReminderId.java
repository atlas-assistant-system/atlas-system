package atlas.domain.appointments.entities;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.UUID;

public record ReminderId(UUID value) implements SingleValueObject<UUID> {

    public ReminderId {
        ObjectGuard.notNull(value, "value");
    }

    public static ReminderId of(UUID value) {
        return new ReminderId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
