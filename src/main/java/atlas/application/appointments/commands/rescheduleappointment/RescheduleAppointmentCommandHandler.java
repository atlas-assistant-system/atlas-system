package atlas.application.appointments.commands.rescheduleappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public final class RescheduleAppointmentCommandHandler
    implements CommandHandler<RescheduleAppointmentCommand, Result<AppointmentDto>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final AppointmentAvailability availability;
    private final Clock clock;

    public RescheduleAppointmentCommandHandler(
        AppointmentUnitOfWork unitOfWork, AppointmentAvailability availability, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.availability = availability;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(RescheduleAppointmentCommand command) {
        var timeSlotResult = TimeSlot.create(command.newStart(), command.newEnd());
        if (timeSlotResult.isFailure()) {
            return Result.failure(timeSlotResult.error());
        }

        var occurredOn = clock.instant();
        var now = LocalDateTime.ofInstant(occurredOn, clock.getZone());

        return unitOfWork.execute(() -> reschedule(command, timeSlotResult.value(), now, occurredOn));
    }

    private Result<AppointmentDto> reschedule(
        RescheduleAppointmentCommand command, TimeSlot newTimeSlot, LocalDateTime now, Instant occurredOn) {

        var appointments = unitOfWork.appointments();
        var appointment = appointments.get(command.appointmentId());
        if (appointment.isEmpty()) {
            return Result.failure(AppointmentErrors.notFound(command.appointmentId()));
        }

        var booked = appointments.findActiveInWindow(newTimeSlot).stream()
            .filter(other -> !other.id().equals(command.appointmentId()))
            .map(Appointment::toBookedSlot)
            .toList();

        var conflictsResult = availability.ensureSchedulable(newTimeSlot, booked, command.allowOverlap());
        if (conflictsResult.isFailure()) {
            return Result.failure(conflictsResult.error());
        }

        var rescheduleResult = appointment.get().reschedule(newTimeSlot, now, occurredOn);
        if (rescheduleResult.isFailure()) {
            return Result.failure(rescheduleResult.error());
        }

        appointments.update(appointment.get());

        return Result.success(AppointmentMapper.toDto(appointment.get(), conflictsResult.value()));
    }
}
