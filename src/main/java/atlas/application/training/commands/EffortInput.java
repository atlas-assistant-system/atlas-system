package atlas.application.training.commands;

import java.math.BigDecimal;

/** Las cuatro medidas tal y como llegan del borde: la carga todavía en kilos. */
public record EffortInput(BigDecimal load, int reps, int seconds, int meters) {}
