package atlas.application.appointments.commands.changeappointmentdetails;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class ChangeAppointmentDetailsCommandHandler
    implements CommandHandler<ChangeAppointmentDetailsCommand, Result<AppointmentDto>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final Clock clock;

    public ChangeAppointmentDetailsCommandHandler(AppointmentUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(ChangeAppointmentDetailsCommand command) {
        var titleResult = AppointmentTitle.create(command.title());
        if (titleResult.isFailure()) {
            return Result.failure(titleResult.error());
        }

        var descriptionResult = AppointmentDescription.create(command.description());
        if (descriptionResult.isFailure()) {
            return Result.failure(descriptionResult.error());
        }

        var occurredOn = clock.instant();

        return unitOfWork.execute(() -> {
            var appointments = unitOfWork.appointments();
            var appointment = appointments.get(command.appointmentId());
            if (appointment.isEmpty()) {
                return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
            }

            var result =
                appointment.get().changeDetails(titleResult.value(), descriptionResult.value(), occurredOn);
            if (result.isFailure()) {
                return Result.failure(result.error());
            }

            appointments.update(appointment.get());

            return Result.success(AppointmentMapper.toDto(appointment.get()));
        });
    }
}
