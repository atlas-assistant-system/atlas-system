package atlas.domain.appointments.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalendarPeriodTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 8, 19);
    private static final LocalDate MONDAY_OF_THAT_WEEK = LocalDate.of(2026, 8, 17);

    @Test
    void shouldSpanASingleDayForDay() {
        var window = CalendarPeriod.DAY.windowFor(WEDNESDAY);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 8, 19, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2026, 8, 20, 0, 0));
    }

    @Test
    void shouldStartTheWeekOnMonday() {
        assertThat(CalendarPeriod.WEEK.startOf(WEDNESDAY)).isEqualTo(MONDAY_OF_THAT_WEEK);
        assertThat(CalendarPeriod.WEEK.startOf(MONDAY_OF_THAT_WEEK)).isEqualTo(MONDAY_OF_THAT_WEEK);
    }

    @Test
    void shouldSpanSevenDaysForWeek() {
        var window = CalendarPeriod.WEEK.windowFor(WEDNESDAY);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 8, 17, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2026, 8, 24, 0, 0));
    }

    @Test
    void shouldSpanTheWholeMonthForMonth() {
        var window = CalendarPeriod.MONTH.windowFor(WEDNESDAY);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
    }

    @Test
    void shouldSpanTheWholeYearForYear() {
        var window = CalendarPeriod.YEAR.windowFor(WEDNESDAY);

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    @Test
    void shouldNotDriftWhenWalkingMonthsFromADayThatOtherMonthsLack() {
        var january31 = LocalDate.of(2026, 1, 31);

        var february = CalendarPeriod.MONTH.next(january31);
        var march = CalendarPeriod.MONTH.next(february);

        assertThat(february).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(march).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void shouldWalkBackAndForthToTheSamePeriod() {
        for (var period : CalendarPeriod.values()) {
            var start = period.startOf(WEDNESDAY);

            assertThat(period.previous(period.next(start)))
                .as("%s should round-trip", period)
                .isEqualTo(start);
        }
    }

    @Test
    void shouldMakeConsecutiveWindowsMeetWithoutGapOrOverlap() {
        for (var period : CalendarPeriod.values()) {
            var current = period.windowFor(WEDNESDAY);
            var following = period.windowFor(period.next(WEDNESDAY));

            assertThat(current.end()).as("%s windows should be contiguous", period).isEqualTo(following.start());
            assertThat(current.overlaps(following))
                .as("%s windows should not overlap", period)
                .isFalse();
        }
    }
}
