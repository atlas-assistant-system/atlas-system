package atlas.application.appointments.dto;

import java.time.LocalDateTime;

public record FreeSlotDto(LocalDateTime start, LocalDateTime end) {}
