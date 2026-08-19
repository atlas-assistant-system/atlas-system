package atlas.application.appointments.commands.changeappointmentdetails;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record ChangeAppointmentDetailsCommand(AppointmentId appointmentId, String title, String description)
    implements Command<Result<AppointmentDto>> {}
