package atlas.application.appointments.commands.addreminder;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.appointments.ports.ReminderIdGenerator;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDateTime;

public final class AddReminderCommandHandler
    implements CommandHandler<AddReminderCommand, Result<AppointmentDto>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final ReminderIdGenerator reminderIds;
    private final Clock clock;

    public AddReminderCommandHandler(AppointmentUnitOfWork unitOfWork, ReminderIdGenerator reminderIds, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.reminderIds = reminderIds;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(AddReminderCommand command) {
        var leadTimeResult = ReminderLeadTime.create(command.leadTimeMinutes());
        if (leadTimeResult.isFailure()) {
            return Result.failure(leadTimeResult.error());
        }

        var occurredOn = clock.instant();
        var now = LocalDateTime.ofInstant(occurredOn, clock.getZone());

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            var result =
                appointment.get().addReminder(reminderIds.next(), leadTimeResult.value(), now, occurredOn);
            if (result.isFailure()) {
                return Result.failure(result.error());
            }

            appointments.update(appointment.get());

            return Result.success(AppointmentMapper.toDto(appointment.get()));
        });
    }
}
