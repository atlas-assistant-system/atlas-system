package atlas.application.appointments.dto;

import java.time.Instant;

public record ReminderDto(String id, int leadTimeMinutes, Instant acknowledgedAt) {}
