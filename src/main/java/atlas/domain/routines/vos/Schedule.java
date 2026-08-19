package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.CommonErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record Schedule(RecurrencePeriod period, Set<DayOfWeek> activeDays, Set<Integer> daysOfMonth)
    implements ValueObject {

    public static final int LAST_DAY = 0;

    public Schedule {
        ObjectGuard.notNull(period, "period");
        activeDays = Set.copyOf(activeDays);
        daysOfMonth = Set.copyOf(daysOfMonth);
    }

    public static Result<Schedule> create(
        RecurrencePeriod period,
        Set<DayOfWeek> activeDays,
        Set<Integer> daysOfMonth) {

        if (period == null) {
            return Result.failure(CommonErrors.required("period"));
        }

        var weekdays = activeDays == null ? Set.<DayOfWeek>of() : activeDays;
        var monthDays = daysOfMonth == null ? Set.<Integer>of() : daysOfMonth;

        if (period == RecurrencePeriod.DAY && weekdays.isEmpty()) {
            return Result.failure(RoutineErrors.ACTIVE_DAYS_REQUIRED_FOR_DAILY_ROUTINE);
        }

        if (period != RecurrencePeriod.DAY && !weekdays.isEmpty()) {
            return Result.failure(RoutineErrors.ACTIVE_DAYS_NOT_ALLOWED_FOR_THIS_PERIOD);
        }

        if (period != RecurrencePeriod.MONTH && !monthDays.isEmpty()) {
            return Result.failure(RoutineErrors.DAYS_OF_MONTH_NOT_ALLOWED_FOR_THIS_PERIOD);
        }

        if (monthDays.stream().anyMatch(day -> day == null || day < LAST_DAY || day > 31)) {
            return Result.failure(RoutineErrors.DAY_OF_MONTH_OUT_OF_RANGE);
        }

        return Result.success(new Schedule(period, weekdays, monthDays));
    }

    public static Result<Schedule> daily(Set<DayOfWeek> activeDays) {
        return create(RecurrencePeriod.DAY, activeDays, Set.of());
    }

    public static Result<Schedule> over(RecurrencePeriod period) {
        return create(period, Set.of(), Set.of());
    }

    public boolean includes(LocalDate day) {
        return (activeDays.isEmpty() || activeDays.contains(day.getDayOfWeek()))
            && (daysOfMonth.isEmpty() || matchesDayOfMonth(day));
    }

    public boolean occursIn(PeriodWindow window) {
        return window.days().anyMatch(this::includes);
    }

    public PeriodWindow windowFor(LocalDate day) {
        return period.windowFor(day);
    }

    private boolean matchesDayOfMonth(LocalDate day) {
        return daysOfMonth.contains(day.getDayOfMonth())
            || (daysOfMonth.contains(LAST_DAY) && day.getDayOfMonth() == day.lengthOfMonth());
    }
}
