package atlas.application.appointments.dto;

import java.time.LocalDate;

public record DailyAppointmentCountDto(LocalDate day, long count) {}
