package atlas.application.appointments.commands.deleteappointment;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteAppointmentCommand(AppointmentId appointmentId) implements Command<Result<Void>> {}
