package atlas.application.appointments.commands.scheduleappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.appointments.ports.ReminderIdGenerator;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ScheduleAppointmentCommandHandler
    implements CommandHandler<ScheduleAppointmentCommand, Result<AppointmentDto>> {

    private final AppointmentUnitOfWork unitOfWork;
    private final AppointmentAvailability availability;
    private final ReminderIdGenerator reminderIds;
    private final Clock clock;

    public ScheduleAppointmentCommandHandler(
        AppointmentUnitOfWork unitOfWork,
        AppointmentAvailability availability,
        ReminderIdGenerator reminderIds,
        Clock clock) {
        this.unitOfWork = unitOfWork;
        this.availability = availability;
        this.reminderIds = reminderIds;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(ScheduleAppointmentCommand command) {
        var titleResult = AppointmentTitle.create(command.title());
        if (titleResult.isFailure()) {
            return Result.failure(titleResult.error());
        }

        var descriptionResult = AppointmentDescription.create(command.description());
        if (descriptionResult.isFailure()) {
            return Result.failure(descriptionResult.error());
        }

        var timeSlotResult = TimeSlot.create(command.start(), command.end());
        if (timeSlotResult.isFailure()) {
            return Result.failure(timeSlotResult.error());
        }

        var leadTimesResult = createLeadTimes(command.reminderLeadTimesMinutes());
        if (leadTimesResult.isFailure()) {
            return Result.failure(leadTimesResult.error());
        }

        var occurredOn = clock.instant();
        var now = LocalDateTime.ofInstant(occurredOn, clock.getZone());

        return unitOfWork.execute(() -> schedule(
            titleResult.value(), descriptionResult.value(), timeSlotResult.value(), leadTimesResult.value(),
            command.allowOverlap(), now, occurredOn));
    }

    private Result<AppointmentDto> schedule(
        AppointmentTitle title,
        Optional<AppointmentDescription> description,
        TimeSlot timeSlot,
        List<ReminderLeadTime> leadTimes,
        boolean allowOverlap,
        LocalDateTime now,
        Instant occurredOn) {

        var appointments = unitOfWork.appointments();
        var booked = appointments.findActiveInWindow(timeSlot).stream().map(Appointment::toBookedSlot).toList();

        var conflictsResult = availability.ensureSchedulable(timeSlot, booked, allowOverlap);
        if (conflictsResult.isFailure()) {
            return Result.failure(conflictsResult.error());
        }

        var appointmentResult =
            Appointment.schedule(appointments.nextId(), title, description, timeSlot, now, occurredOn);
        if (appointmentResult.isFailure()) {
            return Result.failure(appointmentResult.error());
        }

        var appointment = appointmentResult.value();
        for (var leadTime : leadTimes) {
            var reminderResult = appointment.addReminder(reminderIds.next(), leadTime, now, occurredOn);
            if (reminderResult.isFailure()) {
                return Result.failure(reminderResult.error());
            }
        }

        appointments.create(appointment);

        return Result.success(AppointmentMapper.toDto(appointment, conflictsResult.value()));
    }

    private Result<List<ReminderLeadTime>> createLeadTimes(List<Integer> minutes) {
        var leadTimes = new ArrayList<ReminderLeadTime>();

        for (var value : minutes == null ? List.<Integer>of() : minutes) {
            if (value == null) {
                return Result.failure(AppointmentErrors.INVALID_REMINDER_LEAD_TIME);
            }

            var result = ReminderLeadTime.create(value);
            if (result.isFailure()) {
                return Result.failure(result.error());
            }

            leadTimes.add(result.value());
        }

        return Result.success(List.copyOf(leadTimes));
    }
}
