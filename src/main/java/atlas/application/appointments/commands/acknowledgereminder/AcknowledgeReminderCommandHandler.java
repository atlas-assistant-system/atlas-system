package atlas.application.appointments.commands.acknowledgereminder;

import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class AcknowledgeReminderCommandHandler
    implements CommandHandler<AcknowledgeReminderCommand, Result<Void>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final Clock clock;

    public AcknowledgeReminderCommandHandler(AppointmentUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(AcknowledgeReminderCommand command) {
        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            var result = appointment.get().acknowledgeReminder(command.reminderId(), occurredOn);
            if (result.isFailure()) {
                return result;
            }

            appointments.update(appointment.get());

            return Result.success();
        });
    }
}
