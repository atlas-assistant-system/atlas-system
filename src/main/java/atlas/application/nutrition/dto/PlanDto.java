package atlas.application.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PlanDto(
    String id,
    BigDecimal startWeight,
    BigDecimal targetWeight,
    String goal,
    String goalLabel,
    MacrosDto dailyMacros,
    String status,
    LocalDate startedOn,
    Instant definedAt) {}
