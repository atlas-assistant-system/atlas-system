package atlas.application.appointments.commands.rescheduleappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDateTime;

public record RescheduleAppointmentCommand(
    AppointmentId appointmentId, LocalDateTime newStart, LocalDateTime newEnd, boolean allowOverlap)
    implements Command<Result<AppointmentDto>> {}
