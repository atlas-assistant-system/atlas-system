package atlas.application.appointments.commands.restoreappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public final class RestoreAppointmentCommandHandler
    implements CommandHandler<RestoreAppointmentCommand, Result<AppointmentDto>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final AppointmentAvailability availability;
    private final Clock clock;

    public RestoreAppointmentCommandHandler(
        AppointmentUnitOfWork unitOfWork, AppointmentAvailability availability, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.availability = availability;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(RestoreAppointmentCommand command) {
        var occurredOn = clock.instant();
        var now = LocalDateTime.ofInstant(occurredOn, clock.getZone());

        return unitOfWork.execute(() -> restore(command, now, occurredOn));
    }

    private Result<AppointmentDto> restore(
        RestoreAppointmentCommand command, LocalDateTime now, Instant occurredOn) {

        var appointments = unitOfWork.appointments();
        var appointment = appointments.get(command.appointmentId());
        if (appointment.isEmpty()) {
            return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
        }

        var restoreResult = appointment.get().restore(now, occurredOn);
        if (restoreResult.isFailure()) {
            return Result.failure(restoreResult.error());
        }

        var timeSlot = appointment.get().timeSlot();
        var booked = appointments.findActiveInWindow(timeSlot).stream()
            .filter(other -> !other.id().equals(command.appointmentId()))
            .map(Appointment::toBookedSlot)
            .toList();

        var conflictsResult = availability.ensureSchedulable(timeSlot, booked, command.allowOverlap());
        if (conflictsResult.isFailure()) {
            return Result.failure(conflictsResult.error());
        }

        appointments.update(appointment.get());

        return Result.success(AppointmentMapper.toDto(appointment.get(), conflictsResult.value()));
    }
}
