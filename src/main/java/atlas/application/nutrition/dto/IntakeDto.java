package atlas.application.nutrition.dto;

import java.time.Instant;
import java.time.LocalDate;

public record IntakeDto(
    String id,
    MacrosDto macros,
    String note,
    LocalDate consumedOn,
    Instant recordedAt) {}
