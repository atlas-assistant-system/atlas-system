package atlas.application.appointments.commands.restoreappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record RestoreAppointmentCommand(AppointmentId appointmentId, boolean allowOverlap)
    implements Command<Result<AppointmentDto>> {}
