package atlas.application.routines.dto;

import java.math.BigDecimal;

public record RoutineSummaryDto(
    String id,
    String name,
    BigDecimal target,
    String unit,
    String period,
    boolean archived) {}
