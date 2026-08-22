package atlas.application.training.dto;

import java.math.BigDecimal;

/** La carga sale en kilos: la conversión a gramos es del borde, no de quien consume la API. */
public record EffortDto(BigDecimal load, int reps, int seconds, int meters) {}
