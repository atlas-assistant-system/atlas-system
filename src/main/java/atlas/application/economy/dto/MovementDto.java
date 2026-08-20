package atlas.application.economy.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MovementDto(
    String id,
    String kind,
    BigDecimal amount,
    String category,
    String categoryLabel,
    String categoryIcon,
    String note,
    LocalDate occurredOn,
    Instant recordedAt) {}
