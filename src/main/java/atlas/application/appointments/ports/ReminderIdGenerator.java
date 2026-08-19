package atlas.application.appointments.ports;

import atlas.domain.appointments.entities.ReminderId;

public interface ReminderIdGenerator {

    ReminderId next();
}
