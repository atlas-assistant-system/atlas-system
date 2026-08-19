package atlas.domain.appointments.events;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;
import java.util.Optional;

public record AppointmentDetailsChangedEvent(
    AppointmentId appointmentId,
    AppointmentTitle title,
    Optional<AppointmentDescription> description,
    Instant occurredOn)
    implements DomainEvent {}
