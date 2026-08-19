package atlas.domain.appointments.events;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record ReminderRemovedEvent(AppointmentId appointmentId, ReminderId reminderId, Instant occurredOn)
    implements DomainEvent {}
