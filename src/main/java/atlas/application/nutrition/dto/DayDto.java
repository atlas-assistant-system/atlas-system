package atlas.application.nutrition.dto;

import java.time.LocalDate;
import java.util.List;

public record DayDto(
    LocalDate date,
    int consumedCalories,
    MacrosDto consumedMacros,
    Integer targetCalories,
    MacrosDto targetMacros,
    Integer remainingCalories,
    MacrosDto remainingMacros,
    int caloriePercentage,
    int lowerCalories,
    int upperCalories,
    boolean withinRange,
    boolean overBudget,
    List<IntakeDto> intakes) {}
