package atlas.domain.routines.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.enums.RecurrencePeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScheduleTest {

    private static final Set<DayOfWeek> WORKOUT_DAYS =
        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

    @Test
    void shouldFailWhenDailyRoutineHasNoActiveDays() {
        var result = Schedule.create(RecurrencePeriod.DAY, Set.of(), Set.of());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(RoutineErrors.ACTIVE_DAYS_REQUIRED_FOR_DAILY_ROUTINE);
    }

    @Test
    void shouldFailWhenDailyRoutineGetsNullActiveDays() {
        assertThat(Schedule.create(RecurrencePeriod.DAY, null, null).isFailure()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WEEK", "MONTH"})
    void shouldFailWhenWeekdaysAreRestrictedOutsideADailyRoutine(RecurrencePeriod period) {
        var result = Schedule.create(period, WORKOUT_DAYS, Set.of());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(RoutineErrors.ACTIVE_DAYS_NOT_ALLOWED_FOR_THIS_PERIOD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DAY", "WEEK"})
    void shouldFailWhenMonthDaysAreRestrictedOutsideAMonthlyRoutine(RecurrencePeriod period) {
        var activeDays = period == RecurrencePeriod.DAY ? WORKOUT_DAYS : Set.<DayOfWeek>of();

        var result = Schedule.create(period, activeDays, Set.of(1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(RoutineErrors.DAYS_OF_MONTH_NOT_ALLOWED_FOR_THIS_PERIOD);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 32, 99})
    void shouldFailWhenDayOfMonthIsOutOfRange(int dayOfMonth) {
        var result = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(dayOfMonth));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(RoutineErrors.DAY_OF_MONTH_OUT_OF_RANGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {Schedule.LAST_DAY, 1, 15, 31})
    void shouldAcceptEveryDayOfMonthInRange(int dayOfMonth) {
        assertThat(Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(dayOfMonth)).isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenPeriodIsMissing() {
        assertThat(Schedule.create(null, Set.of(), Set.of()).isFailure()).isTrue();
    }

    @Test
    void shouldAllowAMonthlyRoutineWithoutRestrictingDays() {
        assertThat(Schedule.over(RecurrencePeriod.MONTH).isSuccess()).isTrue();
    }

    @Test
    void shouldNotBeAffectedByLaterMutationOfTheGivenSets() {
        var days = new HashSet<>(WORKOUT_DAYS);
        var schedule = Schedule.daily(days).value();

        days.add(DayOfWeek.SUNDAY);

        assertThat(schedule.activeDays()).isEqualTo(WORKOUT_DAYS);
        assertThatThrownBy(() -> schedule.activeDays().add(DayOfWeek.SUNDAY))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{0} es elegible en una rutina L-X-V: {1}")
    @CsvSource({
        "2026-02-09, true",  // lunes
        "2026-02-10, false", // martes
        "2026-02-11, true",  // miercoles
        "2026-02-13, true",  // viernes
        "2026-02-15, false"}) // domingo
    void shouldIncludeOnlyActiveWeekdaysWhenPeriodIsDay(LocalDate day, boolean expected) {
        var schedule = Schedule.daily(WORKOUT_DAYS).value();

        assertThat(schedule.includes(day)).isEqualTo(expected);
    }

    @Test
    void shouldIncludeAnyDayWhenPeriodIsWeek() {
        var schedule = Schedule.over(RecurrencePeriod.WEEK).value();

        assertThat(schedule.includes(LocalDate.of(2026, 2, 10))).isTrue();
        assertThat(schedule.includes(LocalDate.of(2026, 2, 15))).isTrue();
    }

    @Test
    void shouldIncludeAnyDayWhenMonthlyRoutineDoesNotRestrictDays() {
        var schedule = Schedule.over(RecurrencePeriod.MONTH).value();

        assertThat(schedule.includes(LocalDate.of(2026, 2, 17))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "2026-02-01, true",
        "2026-02-02, false",
        "2026-03-01, true"})
    void shouldIncludeOnlyTheRequestedDayOfMonth(LocalDate day, boolean expected) {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(1)).value();

        assertThat(schedule.includes(day)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "2026-01-31, true",
        "2026-02-28, false", // febrero no tiene 31 — no toca
        "2026-04-30, false", // abril tampoco
        "2026-05-31, true"})
    void shouldNotClampDayThirtyOneToTheEndOfShorterMonths(LocalDate day, boolean expected) {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(31)).value();

        assertThat(schedule.includes(day)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} es ultimo dia de mes: {1}")
    @CsvSource({
        "2026-01-31, true",  // 31 dias
        "2026-02-28, true",  // 28 dias
        "2028-02-29, true",  // 29 dias, bisiesto
        "2026-04-30, true",  // 30 dias
        "2028-02-28, false", // en bisiesto el 28 ya no es el ultimo
        "2026-01-30, false",
        "2026-12-31, true"})
    void shouldIncludeTheLastDayOfEveryMonthWhateverItsLength(LocalDate day, boolean expected) {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(Schedule.LAST_DAY)).value();

        assertThat(schedule.includes(day)).isEqualTo(expected);
    }

    @Test
    void shouldIncludeExactlyOneDayPerMonthWhenTargetingTheLastDay() {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(Schedule.LAST_DAY)).value();

        var included = LocalDate.of(2028, 2, 1)
            .datesUntil(LocalDate.of(2028, 3, 1))
            .filter(schedule::includes)
            .toList();

        assertThat(included).containsExactly(LocalDate.of(2028, 2, 29));
    }

    @Test
    void shouldIncludeBothAFixedDayAndTheLastDayWhenBothAreRequested() {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(1, Schedule.LAST_DAY)).value();

        var included = LocalDate.of(2026, 2, 1)
            .datesUntil(LocalDate.of(2026, 3, 1))
            .filter(schedule::includes)
            .toList();

        assertThat(included).containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void shouldTreatDayThirtyOneAndLastDayAsDifferentThings() {
        var thirtyFirst = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(31)).value();
        var lastDay = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(Schedule.LAST_DAY)).value();
        var endOfFebruary = LocalDate.of(2026, 2, 28);

        assertThat(thirtyFirst.includes(endOfFebruary)).isFalse();
        assertThat(lastDay.includes(endOfFebruary)).isTrue();
        assertThat(thirtyFirst).isNotEqualTo(lastDay);
    }
}
