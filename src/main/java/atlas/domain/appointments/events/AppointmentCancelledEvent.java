package atlas.domain.appointments.events;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record AppointmentCancelledEvent(AppointmentId appointmentId, Instant occurredOn) implements DomainEvent {}
