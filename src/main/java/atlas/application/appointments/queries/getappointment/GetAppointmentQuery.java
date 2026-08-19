package atlas.application.appointments.queries.getappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record GetAppointmentQuery(AppointmentId appointmentId) implements Query<Result<AppointmentDto>> {}
