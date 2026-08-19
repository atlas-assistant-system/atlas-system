package atlas.application.appointments.commands.acknowledgereminder;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.sharedkernel.results.Result;

public record AcknowledgeReminderCommand(AppointmentId appointmentId, ReminderId reminderId)
    implements Command<Result<Void>> {}
