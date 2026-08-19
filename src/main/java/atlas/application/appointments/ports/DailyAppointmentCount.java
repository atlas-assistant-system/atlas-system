package atlas.application.appointments.ports;

import java.time.LocalDate;

public record DailyAppointmentCount(LocalDate day, long count) {}
