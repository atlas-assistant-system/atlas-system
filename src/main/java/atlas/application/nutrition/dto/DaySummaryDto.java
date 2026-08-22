package atlas.application.nutrition.dto;

import java.time.LocalDate;

public record DaySummaryDto(
    LocalDate date,
    int consumedCalories,
    MacrosDto consumedMacros,
    int caloriePercentage,
    boolean withinRange,
    boolean overBudget) {}
