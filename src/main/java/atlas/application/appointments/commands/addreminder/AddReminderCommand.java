package atlas.application.appointments.commands.addreminder;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record AddReminderCommand(AppointmentId appointmentId, int leadTimeMinutes)
    implements Command<Result<AppointmentDto>> {}
