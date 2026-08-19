package atlas.application.appointments.mappers;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.appointments.dto.DailyAppointmentCountDto;
import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.appointments.dto.FreeSlotDto;
import atlas.application.appointments.dto.MonthlyAppointmentCountDto;
import atlas.application.appointments.dto.ReminderDto;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.application.appointments.ports.DueReminder;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.Reminder;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.TimeSlot;
import java.util.List;

public final class AppointmentMapper {

    private AppointmentMapper() {}

    public static AppointmentDto toDto(Appointment appointment) {
        return toDto(appointment, List.of());
    }

    public static AppointmentDto toDto(Appointment appointment, List<AppointmentId> conflictingIds) {
        return new AppointmentDto(
            appointment.id().toString(),
            appointment.title().value(),
            appointment.description().map(AppointmentDescription::value).orElse(null),
            appointment.timeSlot().start(),
            appointment.timeSlot().end(),
            appointment.status().name(),
            appointment.reminders().stream().map(AppointmentMapper::toDto).toList(),
            conflictingIds.stream().map(AppointmentId::toString).toList());
    }

    public static ReminderDto toDto(Reminder reminder) {
        return new ReminderDto(
            reminder.id().toString(), reminder.leadTime().value(), reminder.acknowledgedAt().orElse(null));
    }

    public static AppointmentSummaryDto toDto(AppointmentSummary summary) {
        return new AppointmentSummaryDto(
            summary.id().toString(),
            summary.title().value(),
            summary.slot().start(),
            summary.slot().end(),
            summary.status().name());
    }

    public static MonthlyAppointmentCountDto toDto(MonthlyAppointmentCount count) {
        return new MonthlyAppointmentCountDto(count.month(), count.count());
    }

    public static DailyAppointmentCountDto toDto(DailyAppointmentCount count) {
        return new DailyAppointmentCountDto(count.day(), count.count());
    }

    public static FreeSlotDto toFreeSlotDto(TimeSlot slot) {
        return new FreeSlotDto(slot.start(), slot.end());
    }

    public static DueReminderDto toDto(DueReminder dueReminder) {
        return new DueReminderDto(
            dueReminder.appointmentId().toString(),
            dueReminder.reminderId().toString(),
            dueReminder.title().value(),
            dueReminder.slot().start(),
            dueReminder.leadTime().value());
    }
}
