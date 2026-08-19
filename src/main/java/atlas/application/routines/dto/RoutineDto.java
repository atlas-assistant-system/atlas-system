package atlas.application.routines.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoutineDto(
    String id,
    String name,
    String description,
    BigDecimal target,
    String unit,
    String period,
    List<String> activeDays,
    List<Integer> daysOfMonth,
    boolean archived) {}
