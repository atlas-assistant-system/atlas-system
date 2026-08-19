package atlas.domain.appointments;

import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.sharedkernel.results.Error;
import java.util.List;

public final class AppointmentErrors {

    public static final Error TITLE_REQUIRED =
        Error.validation("Appointment.TitleRequired", "A title is required.");

    public static final Error TITLE_TOO_LONG =
        Error.validation("Appointment.TitleTooLong", "The title is too long.");

    public static final Error DESCRIPTION_TOO_LONG =
        Error.validation("Appointment.DescriptionTooLong", "The description is too long.");

    public static final Error TIME_SLOT_REQUIRED =
        Error.validation("Appointment.TimeSlotRequired", "A time slot is required.");

    public static final Error INVALID_TIME_SLOT =
        Error.validation("Appointment.InvalidTimeSlot", "The end of a time slot must be after its start.");

    public static final Error CANNOT_SCHEDULE_IN_THE_PAST =
        Error.validation("Appointment.CannotScheduleInThePast", "An appointment cannot start in the past.");

    public static final Error INVALID_REMINDER_LEAD_TIME =
        Error.validation("Appointment.InvalidReminderLeadTime", "The reminder lead time is out of range.");

    public static final Error CANNOT_MODIFY_CANCELLED =
        Error.conflict("Appointment.CannotModifyCancelled", "A cancelled appointment cannot be modified.");

    public static final Error NOT_CANCELLED =
        Error.conflict("Appointment.NotCancelled", "Only a cancelled appointment can be restored.");

    public static final Error CANNOT_RESTORE_PAST_APPOINTMENT = Error.conflict(
        "Appointment.CannotRestorePastAppointment", "An appointment that already started cannot be restored.");

    public static final Error CANNOT_MODIFY_PAST_APPOINTMENT =
        Error.conflict("Appointment.CannotModifyPastAppointment", "A past appointment cannot be modified.");

    public static final Error CANNOT_ADD_REMINDER_TO_PAST_APPOINTMENT = Error.conflict(
        "Appointment.CannotAddReminderToPastAppointment",
        "A reminder cannot be added to an appointment that already happened.");

    public static final Error DUPLICATE_REMINDER_LEAD_TIME = Error.conflict(
        "Appointment.DuplicateReminderLeadTime",
        "The appointment already has a reminder with that lead time.");

    public static final Error TOO_MANY_REMINDERS = Error.conflict(
        "Appointment.TooManyReminders",
        "The appointment already has the maximum number of reminders.");

    public static Error overlaps(List<AppointmentId> conflictingIds) {
        var ids = conflictingIds.stream().map(AppointmentId::toString).toList();

        return Error.conflict(
            "Appointment.Overlaps",
            "The time slot overlaps with existing appointments: " + String.join(", ", ids) + ".");
    }

    public static Error reminderNotFound(ReminderId id) {
        return Error.notFound(
            "Appointment.ReminderNotFound", "Reminder '" + id + "' was not found in this appointment.");
    }

    public static Error notFound(AppointmentId id) {
        return Error.notFound("Appointment.NotFound", "Appointment '" + id + "' was not found.");
    }

    private AppointmentErrors() {}
}
