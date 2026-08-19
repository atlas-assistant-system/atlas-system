package atlas.application.appointments.ports;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;

public record AppointmentSummary(AppointmentId id, AppointmentTitle title, TimeSlot slot, AppointmentStatus status) {}
