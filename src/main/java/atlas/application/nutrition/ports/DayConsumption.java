package atlas.application.nutrition.ports;

import atlas.domain.nutrition.vos.Macros;
import java.time.LocalDate;

public record DayConsumption(LocalDate date, Macros macros) {}
