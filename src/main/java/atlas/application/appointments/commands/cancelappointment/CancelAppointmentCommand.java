package atlas.application.appointments.commands.cancelappointment;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record CancelAppointmentCommand(AppointmentId appointmentId) implements Command<Result<Void>> {}
