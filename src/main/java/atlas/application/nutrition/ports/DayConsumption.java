package atlas.application.nutrition.ports;

import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.Macros;
import java.time.LocalDate;

public record DayConsumption(LocalDate date, Calories calories, Macros macros) {}
