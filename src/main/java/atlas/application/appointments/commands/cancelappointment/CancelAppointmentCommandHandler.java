package atlas.application.appointments.commands.cancelappointment;

import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDateTime;

public final class CancelAppointmentCommandHandler
    implements CommandHandler<CancelAppointmentCommand, Result<Void>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final Clock clock;

    public CancelAppointmentCommandHandler(AppointmentUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(CancelAppointmentCommand command) {
        var occurredOn = clock.instant();
        var now = LocalDateTime.ofInstant(occurredOn, clock.getZone());

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            var result = appointment.get().cancel(now, occurredOn);
            if (result.isFailure()) {
                return result;
            }

            appointments.update(appointment.get());

            return Result.success();
        });
    }
}
