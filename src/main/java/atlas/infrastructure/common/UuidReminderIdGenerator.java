package atlas.infrastructure.common;

import atlas.application.appointments.ports.ReminderIdGenerator;
import atlas.domain.appointments.entities.ReminderId;
import java.util.UUID;

public final class UuidReminderIdGenerator implements ReminderIdGenerator {

    @Override
    public ReminderId next() {
        return ReminderId.of(UUID.randomUUID());
    }
}
