package atlas.application.nutrition.dto;

import java.time.LocalDate;

public record DaySummaryDto(
    LocalDate date,
    MacrosDto consumed,
    int caloriePercentage,
    boolean withinRange,
    boolean overBudget) {}
