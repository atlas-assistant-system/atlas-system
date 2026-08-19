package atlas.domain.appointments.events;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record AppointmentScheduledEvent(AppointmentId appointmentId, TimeSlot timeSlot, Instant occurredOn)
    implements DomainEvent {}
