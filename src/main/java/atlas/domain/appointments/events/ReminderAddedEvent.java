package atlas.domain.appointments.events;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record ReminderAddedEvent(
    AppointmentId appointmentId, ReminderId reminderId, ReminderLeadTime leadTime, Instant occurredOn)
    implements DomainEvent {}
