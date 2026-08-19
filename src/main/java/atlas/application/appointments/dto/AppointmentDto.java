package atlas.application.appointments.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AppointmentDto(
    String id,
    String title,
    String description,
    LocalDateTime start,
    LocalDateTime end,
    String status,
    List<ReminderDto> reminders,
    List<String> conflictingAppointmentIds) {}
