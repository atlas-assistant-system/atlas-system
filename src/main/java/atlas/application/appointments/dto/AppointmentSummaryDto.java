package atlas.application.appointments.dto;

import java.time.LocalDateTime;

public record AppointmentSummaryDto(String id, String title, LocalDateTime start, LocalDateTime end, String status) {}
