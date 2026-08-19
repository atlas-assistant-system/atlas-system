package atlas.application.appointments.ports;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;

public record DueReminder(
    AppointmentId appointmentId, ReminderId reminderId, AppointmentTitle title, TimeSlot slot,
    ReminderLeadTime leadTime) {}
