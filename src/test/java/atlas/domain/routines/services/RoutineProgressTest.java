package atlas.domain.routines.services;

import static atlas.support.builders.RoutineFixtures.daily;
import static atlas.support.builders.RoutineFixtures.entry;
import static atlas.support.builders.RoutineFixtures.monthlyOnDays;
import static atlas.support.builders.RoutineFixtures.routine;
import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Streak;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoutineProgressTest {

    private final RoutineProgress service = new RoutineProgress();

    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);
    private static final LocalDate WED_11 = LocalDate.of(2026, 2, 11);
    private static final LocalDate THU_12 = LocalDate.of(2026, 2, 12);
    private static final LocalDate FRI_13 = LocalDate.of(2026, 2, 13);
    private static final LocalDate SAT_14 = LocalDate.of(2026, 2, 14);

    private static Routine everyDay(int target) {
        return daily(target, DayOfWeek.values());
    }

    @Nested
    class ProgressInAWindow {

        @Test
        void shouldSumEveryEntryInsideTheWindow() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var window = Schedule.over(RecurrencePeriod.WEEK).value().windowFor(WED_11);
            var entries = List.of(entry(MON_09, 1), entry(WED_11, 2));

            var progress = service.progressIn(window, routine, entries);

            assertThat(progress.logged()).isEqualByComparingTo("3");
            assertThat(progress.isMet()).isTrue();
        }

        @Test
        void shouldIgnoreEntriesOutsideTheWindow() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var window = routine.schedule().windowFor(WED_11);
            var entries = List.of(entry(MON_09, 1), entry(LocalDate.of(2026, 2, 16), 5));

            var progress = service.progressIn(window, routine, entries);

            assertThat(progress.logged()).isEqualByComparingTo("1");
            assertThat(progress.isMet()).isFalse();
        }

        @Test
        void shouldIgnoreEntriesOfAnotherRoutine() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var window = routine.schedule().windowFor(WED_11);
            var entries = List.of(entry(RoutineId.of(2), MON_09, 99));

            var progress = service.progressIn(window, routine, entries);

            assertThat(progress.logged()).isEqualByComparingTo("0");
        }

        @Test
        void shouldKeepAmountsBeyondTheQuota() {
            var routine = routine(RecurrencePeriod.WEEK, 2);
            var window = routine.schedule().windowFor(WED_11);

            var progress = service.progressIn(window, routine, List.of(entry(MON_09, 3)));

            assertThat(progress.logged()).isEqualByComparingTo("3");
            assertThat(progress.isMet()).isTrue();
        }

        @Test
        void shouldReportAClosedPeriodWithoutTheQuotaAsFailed() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var window = routine.schedule().windowFor(WED_11);

            var progress = service.progressIn(window, routine, List.of(entry(MON_09, 1)));

            assertThat(progress.isClosedOn(WED_11)).isFalse();
            assertThat(progress.isFailedOn(WED_11)).isFalse();
            assertThat(progress.isClosedOn(LocalDate.of(2026, 2, 16))).isTrue();
            assertThat(progress.isFailedOn(LocalDate.of(2026, 2, 16))).isTrue();
        }

        @Test
        void shouldNotReportAClosedPeriodWithTheQuotaCoveredAsFailed() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var window = routine.schedule().windowFor(WED_11);
            var entries = List.of(entry(MON_09, 1), entry(TUE_10, 1), entry(WED_11, 1));

            var progress = service.progressIn(window, routine, entries);

            assertThat(progress.isFailedOn(LocalDate.of(2026, 2, 16))).isFalse();
        }
    }

    @Nested
    class Streaks {

        @Test
        void shouldBeZeroWhenNothingWasEverLogged() {
            assertThat(service.streakAt(SAT_14, everyDay(1), List.of())).isEqualTo(Streak.NONE);
        }

        @Test
        void shouldBeZeroWhenEveryEntryIsInTheFuture() {
            var entries = List.of(entry(LocalDate.of(2026, 3, 1), 1));

            assertThat(service.streakAt(SAT_14, everyDay(1), entries)).isEqualTo(Streak.NONE);
        }

        @Test
        void shouldCountConsecutiveDaysWhenEveryDayIsCovered() {
            var entries = List.of(entry(THU_12, 1), entry(FRI_13, 1), entry(SAT_14, 1));

            var streak = service.streakAt(SAT_14, everyDay(1), entries);

            assertThat(streak).isEqualTo(new Streak(3, 3));
        }

        @Test
        void shouldBreakOnAClosedDayWithoutTheQuota() {
            var entries = List.of(entry(TUE_10, 1), entry(WED_11, 1), entry(FRI_13, 1), entry(SAT_14, 1));

            var streak = service.streakAt(SAT_14, everyDay(1), entries);

            assertThat(streak).isEqualTo(new Streak(2, 2));
        }

        @Test
        void shouldRememberTheBestRunEvenAfterBreakingIt() {
            var entries = List.of(entry(MON_09, 1), entry(TUE_10, 1), entry(WED_11, 1), entry(FRI_13, 1));

            var streak = service.streakAt(FRI_13, everyDay(1), entries);

            assertThat(streak).isEqualTo(new Streak(1, 3));
        }

        @Test
        void shouldCountTheOpenPeriodOnceItsQuotaIsCovered() {
            var entries = List.of(entry(FRI_13, 1), entry(SAT_14, 1));

            var streak = service.streakAt(SAT_14, everyDay(1), entries);

            assertThat(streak).isEqualTo(new Streak(2, 2));
        }

        @Test
        void shouldNotBreakOnAnOpenPeriodWithoutTheQuota() {
            var entries = List.of(entry(THU_12, 1), entry(FRI_13, 1));

            var streak = service.streakAt(SAT_14, everyDay(1), entries);

            assertThat(streak).isEqualTo(new Streak(2, 2));
        }

        @Test
        void shouldNotTreatAnUnmarkedDayAsAFailureInAWeeklyRoutine() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var entries = List.of(
                entry(LocalDate.of(2026, 2, 3), 1),
                entry(LocalDate.of(2026, 2, 4), 1),
                entry(LocalDate.of(2026, 2, 5), 1),
                entry(MON_09, 1),
                entry(TUE_10, 1));

            var streak = service.streakAt(WED_11, routine, entries);

            assertThat(streak).isEqualTo(new Streak(1, 1));
        }

        @Test
        void shouldCountTheOpenWeekOnceTheQuotaIsCovered() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var entries = List.of(
                entry(LocalDate.of(2026, 2, 3), 1),
                entry(LocalDate.of(2026, 2, 4), 1),
                entry(LocalDate.of(2026, 2, 5), 1),
                entry(MON_09, 1),
                entry(TUE_10, 1),
                entry(WED_11, 1));

            var streak = service.streakAt(WED_11, routine, entries);

            assertThat(streak).isEqualTo(new Streak(2, 2));
        }

        @Test
        void shouldBreakWhenAClosedWeekMissedItsQuota() {
            var routine = routine(RecurrencePeriod.WEEK, 3);
            var entries = List.of(
                entry(LocalDate.of(2026, 2, 3), 1),
                entry(LocalDate.of(2026, 2, 4), 1),
                entry(MON_09, 1),
                entry(TUE_10, 1),
                entry(WED_11, 1));

            var streak = service.streakAt(WED_11, routine, entries);

            assertThat(streak).isEqualTo(new Streak(1, 1));
        }

        @Test
        void shouldSkipDaysThatAreNotScheduled() {
            var routine = daily(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            var entries = List.of(entry(MON_09, 1), entry(WED_11, 1), entry(FRI_13, 1));

            var streak = service.streakAt(FRI_13, routine, entries);

            assertThat(streak).isEqualTo(new Streak(3, 3));
        }

        @Test
        void shouldBreakOnAScheduledDayThatWasMissed() {
            var routine = daily(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            var entries = List.of(entry(MON_09, 1), entry(FRI_13, 1));

            var streak = service.streakAt(FRI_13, routine, entries);

            assertThat(streak).isEqualTo(new Streak(1, 1));
        }

        @Test
        void shouldSkipMonthsWithoutTheRequestedDayOfMonth() {
            var routine = monthlyOnDays(1, 31);
            var entries = List.of(
                entry(LocalDate.of(2026, 1, 31), 1),
                entry(LocalDate.of(2026, 3, 31), 1),
                entry(LocalDate.of(2026, 5, 31), 1));

            var streak = service.streakAt(LocalDate.of(2026, 5, 31), routine, entries);

            assertThat(streak).isEqualTo(new Streak(3, 3));
        }

        @Test
        void shouldCountEveryMonthWhenTargetingItsLastDay() {
            var routine = monthlyOnDays(1, Schedule.LAST_DAY);
            var entries = List.of(
                entry(LocalDate.of(2026, 1, 31), 1),
                entry(LocalDate.of(2026, 2, 28), 1),
                entry(LocalDate.of(2026, 3, 31), 1));

            var streak = service.streakAt(LocalDate.of(2026, 3, 31), routine, entries);

            assertThat(streak).isEqualTo(new Streak(3, 3));
        }

        @Test
        void shouldBreakWhenAMonthWithItsLastDayAvailableWasMissed() {
            var routine = monthlyOnDays(1, Schedule.LAST_DAY);
            var entries = List.of(entry(LocalDate.of(2026, 1, 31), 1), entry(LocalDate.of(2026, 3, 31), 1));

            var streak = service.streakAt(LocalDate.of(2026, 3, 31), routine, entries);

            assertThat(streak).isEqualTo(new Streak(1, 1));
        }

        @Test
        void shouldAddUpSeveralEntriesOfTheSamePeriodTowardsTheQuota() {
            var routine = daily(2, DayOfWeek.values());
            var entries = List.of(entry(SAT_14, 1), entry(SAT_14, 1));

            var streak = service.streakAt(SAT_14, routine, entries);

            assertThat(streak).isEqualTo(new Streak(1, 1));
        }

        @Test
        void shouldIgnoreEntriesOfAnotherRoutine() {
            var entries = List.of(entry(RoutineId.of(2), SAT_14, 5));

            assertThat(service.streakAt(SAT_14, everyDay(1), entries)).isEqualTo(Streak.NONE);
        }
    }
}
