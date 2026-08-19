package atlas.application.appointments.queries.findoverlappingappointments;

import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record FindOverlappingAppointmentsQuery(
    LocalDateTime start, LocalDateTime end, Optional<AppointmentId> excludeAppointmentId)
    implements Query<Result<List<AppointmentSummaryDto>>> {}
