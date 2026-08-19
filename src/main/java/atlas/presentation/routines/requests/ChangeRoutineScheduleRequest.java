package atlas.presentation.routines.requests;

import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.presentation.routines.web.Values;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

public record ChangeRoutineScheduleRequest(
    BigDecimal target,
    String unit,
    RecurrencePeriod period,
    Set<DayOfWeek> activeDays,
    Set<Integer> daysOfMonth) {

    public static ChangeRoutineScheduleRequest from(Map<String, Object> body) {
        return new ChangeRoutineScheduleRequest(
            Values.decimal(body, "target"),
            Values.text(body, "unit"),
            Values.parseEnum(Values.text(body, "period"), RecurrencePeriod.class, "period"),
            Values.weekdays(body, "activeDays"),
            Values.daysOfMonth(body, "daysOfMonth"));
    }
}
