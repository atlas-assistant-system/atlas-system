package atlas.application.appointments.queries.getupcomingappointments;

import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDateTime;
import java.util.List;

public record GetUpcomingAppointmentsQuery(LocalDateTime now, int limit)
    implements Query<Result<List<AppointmentSummaryDto>>> {}
