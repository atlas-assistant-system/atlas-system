package atlas.application.training.commands;

import java.math.BigDecimal;

public record EffortInput(BigDecimal load, int reps, int seconds, int meters) {}
