package atlas.application.routines.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodProgressDto(
    String routineId,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal logged,
    BigDecimal target,
    String unit,
    boolean met,
    boolean closed,
    boolean failed) {}
