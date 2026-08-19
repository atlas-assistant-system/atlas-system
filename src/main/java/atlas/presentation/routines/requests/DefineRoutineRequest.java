package atlas.presentation.routines.requests;

import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.presentation.routines.web.Values;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

public record DefineRoutineRequest(
    String name,
    String description,
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) {

    public static DefineRoutineRequest from(Map<String, Object> body) {
        return new DefineRoutineRequest(
            Values.text(body, "name"),
            Values.text(body, "description"),
            Values.decimal(body, "target"),
            Values.text(body, "unit"),
            Values.parseEnum(Values.text(body, "period"), RecurrencePeriod.class, "period"),
            Values.weekdays(body, "activeDays"),
            Values.daysOfMonth(body, "daysOfMonth"));
    }
}
