package atlas.domain.appointments;

import atlas.domain.appointments.entities.Reminder;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.events.AppointmentCancelledEvent;
import atlas.domain.appointments.events.AppointmentDeletedEvent;
import atlas.domain.appointments.events.AppointmentDetailsChangedEvent;
import atlas.domain.appointments.events.AppointmentRescheduledEvent;
import atlas.domain.appointments.events.AppointmentRestoredEvent;
import atlas.domain.appointments.events.AppointmentScheduledEvent;
import atlas.domain.appointments.events.ReminderAcknowledgedEvent;
import atlas.domain.appointments.events.ReminderAddedEvent;
import atlas.domain.appointments.events.ReminderRemovedEvent;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Appointment extends AggregateRoot<AppointmentId> {

    public static final int MAX_REMINDERS = 5;

    private AppointmentTitle title;
    private AppointmentDescription description;
    private TimeSlot timeSlot;
    private AppointmentStatus status;
    private final List<Reminder> reminders = new ArrayList<>();

    private Appointment(
        AppointmentId id,
        AppointmentTitle title,
        AppointmentDescription description,
        TimeSlot timeSlot,
        AppointmentStatus status) {
        super(id);
        this.title = ObjectGuard.notNull(title, "title");
        this.description = description;
        this.timeSlot = ObjectGuard.notNull(timeSlot, "timeSlot");
        this.status = ObjectGuard.notNull(status, "status");
    }

    public static Result<Appointment> schedule(
        AppointmentId id,
        AppointmentTitle title,
        Optional<AppointmentDescription> description,
        TimeSlot timeSlot,
        LocalDateTime now,
        Instant occurredOn) {

        if (timeSlot.startsInThePast(now)) {
            return Result.failure(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST);
        }

        var appointment =
            new Appointment(id, title, description.orElse(null), timeSlot, AppointmentStatus.SCHEDULED);
        appointment.registerEvent(new AppointmentScheduledEvent(id, timeSlot, occurredOn));

        return Result.success(appointment);
    }

    public static Appointment rehydrate(
        AppointmentId id,
        AppointmentTitle title,
        Optional<AppointmentDescription> description,
        TimeSlot timeSlot,
        AppointmentStatus status,
        List<Reminder> reminders) {

        var appointment = new Appointment(id, title, description.orElse(null), timeSlot, status);
        appointment.reminders.addAll(reminders);

        return appointment;
    }

    public Result<Void> reschedule(TimeSlot newTimeSlot, LocalDateTime now, Instant occurredOn) {
        var modifiable = ensureModifiable(now);
        if (modifiable.isFailure()) {
            return modifiable;
        }

        if (newTimeSlot.startsInThePast(now)) {
            return Result.failure(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST);
        }

        this.timeSlot = newTimeSlot;
        registerEvent(new AppointmentRescheduledEvent(id(), newTimeSlot, occurredOn));

        return Result.success();
    }

    public Result<Void> cancel(LocalDateTime now, Instant occurredOn) {
        var modifiable = ensureModifiable(now);
        if (modifiable.isFailure()) {
            return modifiable;
        }

        this.status = AppointmentStatus.CANCELLED;
        registerEvent(new AppointmentCancelledEvent(id(), occurredOn));

        return Result.success();
    }

    public void delete(Instant occurredOn) {
        registerEvent(new AppointmentDeletedEvent(id(), occurredOn));
    }

    public Result<Void> restore(LocalDateTime now, Instant occurredOn) {
        if (!status.isCancelled()) {
            return Result.failure(AppointmentErrors.NOT_CANCELLED);
        }

        if (timeSlot.startsInThePast(now)) {
            return Result.failure(AppointmentErrors.CANNOT_RESTORE_PAST_APPOINTMENT);
        }

        this.status = AppointmentStatus.SCHEDULED;
        registerEvent(new AppointmentRestoredEvent(id(), timeSlot, occurredOn));

        return Result.success();
    }

    public Result<Void> changeDetails(
        AppointmentTitle newTitle, Optional<AppointmentDescription> newDescription, Instant occurredOn) {

        if (status.isCancelled()) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        }

        this.title = ObjectGuard.notNull(newTitle, "newTitle");
        this.description = newDescription.orElse(null);
        registerEvent(new AppointmentDetailsChangedEvent(id(), newTitle, newDescription, occurredOn));

        return Result.success();
    }

    public Result<Void> addReminder(
        ReminderId reminderId, ReminderLeadTime leadTime, LocalDateTime now, Instant occurredOn) {

        if (status.isCancelled()) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        }

        if (timeSlot.isPast(now)) {
            return Result.failure(AppointmentErrors.CANNOT_ADD_REMINDER_TO_PAST_APPOINTMENT);
        }

        if (reminders.size() >= MAX_REMINDERS) {
            return Result.failure(AppointmentErrors.TOO_MANY_REMINDERS);
        }

        if (hasReminderWith(leadTime)) {
            return Result.failure(AppointmentErrors.DUPLICATE_REMINDER_LEAD_TIME);
        }

        reminders.add(Reminder.create(reminderId, leadTime));
        registerEvent(new ReminderAddedEvent(id(), reminderId, leadTime, occurredOn));

        return Result.success();
    }

    public Result<Void> removeReminder(ReminderId reminderId, Instant occurredOn) {
        if (status.isCancelled()) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        }

        var reminder = findReminder(reminderId);
        if (reminder.isEmpty()) {
            return Result.failure(AppointmentErrors.reminderNotFound(reminderId));
        }

        reminders.remove(reminder.get());
        registerEvent(new ReminderRemovedEvent(id(), reminderId, occurredOn));

        return Result.success();
    }

    public Result<Void> acknowledgeReminder(ReminderId reminderId, Instant occurredOn) {
        var reminder = findReminder(reminderId);
        if (reminder.isEmpty()) {
            return Result.failure(AppointmentErrors.reminderNotFound(reminderId));
        }

        if (reminder.get().acknowledge(occurredOn)) {
            registerEvent(new ReminderAcknowledgedEvent(id(), reminderId, occurredOn));
        }

        return Result.success();
    }

    public AppointmentTitle title() {
        return title;
    }

    public Optional<AppointmentDescription> description() {
        return Optional.ofNullable(description);
    }

    public TimeSlot timeSlot() {
        return timeSlot;
    }

    public AppointmentStatus status() {
        return status;
    }

    public List<Reminder> reminders() {
        return Collections.unmodifiableList(reminders);
    }

    public BookedSlot toBookedSlot() {
        return BookedSlot.of(id(), timeSlot);
    }

    private Result<Void> ensureModifiable(LocalDateTime now) {
        if (status.isCancelled()) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        }

        if (timeSlot.isPast(now)) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_PAST_APPOINTMENT);
        }

        return Result.success();
    }

    private boolean hasReminderWith(ReminderLeadTime leadTime) {
        return reminders.stream().anyMatch(reminder -> reminder.leadTime().equals(leadTime));
    }

    private Optional<Reminder> findReminder(ReminderId reminderId) {
        return reminders.stream().filter(reminder -> reminder.id().equals(reminderId)).findFirst();
    }
}
