package atlas.application.nutrition.dto;

import java.math.BigDecimal;

public record ProgressDto(
    BigDecimal startWeight,
    BigDecimal currentWeight,
    BigDecimal targetWeight,
    String goal,
    String goalLabel,
    BigDecimal remaining,
    int percentage,
    boolean reached,
    BigDecimal trendPerWeek) {}
