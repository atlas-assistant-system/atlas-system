package atlas.application.appointments.dto;

import java.time.LocalDateTime;

public record DueReminderDto(
    String appointmentId, String reminderId, String title, LocalDateTime start, int leadTimeMinutes) {}
