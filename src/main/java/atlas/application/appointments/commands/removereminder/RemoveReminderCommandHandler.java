package atlas.application.appointments.commands.removereminder;

import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class RemoveReminderCommandHandler
    implements CommandHandler<RemoveReminderCommand, Result<Void>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final Clock clock;

    public RemoveReminderCommandHandler(AppointmentUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RemoveReminderCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            var result = appointment.get().removeReminder(command.reminderId(), occurredOn);
            if (result.isFailure()) {
                return result;
            }

            appointments.update(appointment.get());

            return Result.success();
        });
    }
}
