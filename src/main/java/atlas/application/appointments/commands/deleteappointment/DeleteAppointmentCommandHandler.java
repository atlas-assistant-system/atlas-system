package atlas.application.appointments.commands.deleteappointment;

import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class DeleteAppointmentCommandHandler
    implements CommandHandler<DeleteAppointmentCommand, Result<Void>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteAppointmentCommandHandler(AppointmentUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteAppointmentCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            appointment.get().delete(occurredOn);
            appointments.delete(appointment.get());

            return Result.success();
        });
    }
}
