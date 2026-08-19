package atlas.application.routines.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoryDayDto(LocalDate day, BigDecimal logged, boolean scheduled) {}
