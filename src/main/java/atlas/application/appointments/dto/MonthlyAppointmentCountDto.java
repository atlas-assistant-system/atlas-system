package atlas.application.appointments.dto;

import java.time.YearMonth;

public record MonthlyAppointmentCountDto(YearMonth month, long count) {}
