package atlas.domain.routines.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class RecurrencePeriodTest {

    @Test
    void shouldSpanASingleDayWhenPeriodIsDay() {
        var day = LocalDate.of(2026, 2, 14);

        var window = RecurrencePeriod.DAY.windowFor(day);

        assertThat(window.start()).isEqualTo(day);
        assertThat(window.end()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @ParameterizedTest(name = "{0} pertenece a la semana que empieza el {1}")
    @CsvSource({
        "2026-02-09, 2026-02-09", // lunes
        "2026-02-12, 2026-02-09", // jueves
        "2026-02-15, 2026-02-09", // domingo: sigue siendo esa semana, no la siguiente
        "2026-02-16, 2026-02-16"})
    void shouldStartWeeksOnMonday(LocalDate day, LocalDate expectedStart) {
        var window = RecurrencePeriod.WEEK.windowFor(day);

        assertThat(window.start()).isEqualTo(expectedStart);
        assertThat(window.end()).isEqualTo(expectedStart.plusDays(7));
    }

    @Test
    void shouldSpanCalendarMonthWhenPeriodIsMonth() {
        var window = RecurrencePeriod.MONTH.windowFor(LocalDate.of(2026, 1, 31));

        assertThat(window.start()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(window.end()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void shouldSpanShortMonthWhenPeriodIsMonth() {
        var window = RecurrencePeriod.MONTH.windowFor(LocalDate.of(2026, 2, 28));

        assertThat(window.start()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(window.end()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void shouldSpanFebruaryOfALeapYear() {
        var window = RecurrencePeriod.MONTH.windowFor(LocalDate.of(2028, 2, 29));

        assertThat(window.start()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(window.end()).isEqualTo(LocalDate.of(2028, 3, 1));
    }

    @ParameterizedTest
    @CsvSource({
        "DAY,   2026-02-14, 2026-02-15",
        "WEEK,  2026-02-12, 2026-02-16",
        "MONTH, 2026-01-31, 2026-02-01",
        "MONTH, 2026-12-15, 2027-01-01"})
    void shouldReturnStartOfFollowingPeriodOnNext(RecurrencePeriod period, LocalDate anchor, LocalDate expected) {
        assertThat(period.next(anchor)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "DAY,   2026-02-14, 2026-02-13",
        "WEEK,  2026-02-12, 2026-02-02",
        "MONTH, 2026-03-31, 2026-02-01",
        "MONTH, 2026-01-01, 2025-12-01"})
    void shouldReturnStartOfPrecedingPeriodOnPrevious(RecurrencePeriod period, LocalDate anchor, LocalDate expected) {
        assertThat(period.previous(anchor)).isEqualTo(expected);
    }

    @Test
    void shouldNotDriftWhenWalkingThroughShortMonths() {
        var anchor = LocalDate.of(2026, 1, 31);

        var forward = RecurrencePeriod.MONTH.next(anchor);
        var backAgain = RecurrencePeriod.MONTH.previous(forward);

        assertThat(forward).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(backAgain).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void shouldWalkTwelveMonthsForwardAndBackToTheSameMonth() {
        var start = LocalDate.of(2026, 1, 1);

        var cursor = start;
        for (var i = 0; i < 12; i++) {
            cursor = RecurrencePeriod.MONTH.next(cursor);
        }
        for (var i = 0; i < 12; i++) {
            cursor = RecurrencePeriod.MONTH.previous(cursor);
        }

        assertThat(cursor).isEqualTo(start);
    }

    @ParameterizedTest
    @EnumSource(RecurrencePeriod.class)
    void shouldContainItsOwnAnchorAndNothingOfTheNextPeriod(RecurrencePeriod period) {
        var anchor = LocalDate.of(2026, 2, 14);

        var window = period.windowFor(anchor);

        assertThat(window.contains(anchor)).isTrue();
        assertThat(window.contains(window.start())).isTrue();
        assertThat(window.contains(window.end())).isFalse();
        assertThat(window.contains(window.start().minusDays(1))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RecurrencePeriod.class)
    void shouldChainWindowsWithoutGapsOrOverlap(RecurrencePeriod period) {
        var anchor = LocalDate.of(2026, 12, 31);

        var window = period.windowFor(anchor);
        var following = period.windowFor(period.next(anchor));

        assertThat(following.start()).isEqualTo(window.end());
        assertThat(period.previous(following.start())).isEqualTo(window.start());
    }
}
