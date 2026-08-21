package atlas.application.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeighInDto(String id, BigDecimal weight, LocalDate measuredOn, Instant recordedAt) {}
