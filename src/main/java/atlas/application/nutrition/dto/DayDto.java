package atlas.application.nutrition.dto;

import java.time.LocalDate;
import java.util.List;

public record DayDto(
    LocalDate date,
    MacrosDto consumed,
    MacrosDto target,
    MacrosDto remaining,
    int caloriePercentage,
    boolean overBudget,
    List<IntakeDto> intakes) {}
