package atlas.application.training.dto;

import java.math.BigDecimal;


public record EffortDto(BigDecimal load, int reps, int seconds, int meters) {}
