package atlas.application.appointments.ports;

import java.time.YearMonth;

public record MonthlyAppointmentCount(YearMonth month, long count) {}
