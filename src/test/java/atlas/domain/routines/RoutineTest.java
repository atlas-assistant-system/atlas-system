package atlas.domain.routines;

import static atlas.support.builders.RoutineFixtures.NOW;
import static atlas.support.builders.RoutineFixtures.ROUTINE_ID;
import static atlas.support.builders.RoutineFixtures.daily;
import static atlas.support.builders.RoutineFixtures.monthlyOnDays;
import static atlas.support.builders.RoutineFixtures.routine;
import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineArchivedEvent;
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.events.RoutineDeletedEvent;
import atlas.domain.routines.events.RoutineDetailsChangedEvent;
import atlas.domain.routines.events.RoutineScheduleChangedEvent;
import atlas.domain.routines.events.RoutineUnarchivedEvent;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.results.Result;

class RoutineTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 2, 10);

    @Test
    void shouldRaiseDefinedEventWhenRoutineIsDefined() {
        var routine = Routine
            .define(
                ROUTINE_ID,
                RoutineName.create("Tomar pastillas").value(),
                RoutineDescription.create("Con el desayuno").value().orElseThrow(),
                Target.create(BigDecimal.ONE).value(),
                Schedule.daily(Set.of(DayOfWeek.MONDAY)).value(),
                NOW)
            .value();

        assertThat(routine.isArchived()).isFalse();
        assertThat(routine.description()).map(RoutineDescription::value).contains("Con el desayuno");
        assertThat(routine.pendingEvents())
            .singleElement()
            .isEqualTo(new RoutineDefinedEvent(ROUTINE_ID, routine.name(), NOW));
    }

    @Test
    void shouldLeaveDescriptionEmptyWhenNoneIsGiven() {
        assertThat(routine(RecurrencePeriod.WEEK, 3).description()).isEmpty();
    }

    @Test
    void shouldReplaceDetailsAndRaiseEventWhenDetailsChange() {
        var routine = routine(RecurrencePeriod.WEEK, 3);

        var result = routine.changeDetails(RoutineName.create("Correr").value(), null, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.name().value()).isEqualTo("Correr");
        assertThat(routine.description()).isEmpty();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineDetailsChangedEvent(ROUTINE_ID, NOW));
    }

    @Test
    void shouldReplaceScheduleAndTargetAndRaiseEventWhenScheduleChanges() {
        var routine = routine(RecurrencePeriod.WEEK, 3);
        var newSchedule = Schedule.over(RecurrencePeriod.MONTH).value();
        var newTarget = Target.create(BigDecimal.valueOf(10)).value();

        var result = routine.changeSchedule(newSchedule, newTarget, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.schedule()).isEqualTo(newSchedule);
        assertThat(routine.target()).isEqualTo(newTarget);
        assertThat(routine.pendingEvents())
            .containsExactly(new RoutineScheduleChangedEvent(ROUTINE_ID, newSchedule, NOW));
    }

    @Test
    void shouldFailWhenScheduleChangesOnAnArchivedRoutine() {
        var routine = archived();

        var result = routine.changeSchedule(
            Schedule.over(RecurrencePeriod.MONTH).value(),
            Target.create(BigDecimal.ONE).value(),
            NOW);

        assertThat(result.error()).isEqualTo(RoutineErrors.ROUTINE_IS_ARCHIVED);
    }

    @Test
    void shouldArchiveAndRaiseEvent() {
        var routine = routine(RecurrencePeriod.WEEK, 3);

        var result = routine.archive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.isArchived()).isTrue();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineArchivedEvent(ROUTINE_ID, NOW));
    }

    @Test
    void shouldFailWhenArchivingAnAlreadyArchivedRoutine() {
        assertThat(archived().archive(NOW).error()).isEqualTo(RoutineErrors.ROUTINE_ALREADY_ARCHIVED);
    }

    @Test
    void shouldUnarchiveAndRaiseEvent() {
        var routine = archived();

        var result = routine.unarchive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.isArchived()).isFalse();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineUnarchivedEvent(ROUTINE_ID, NOW));
    }

    @Test
    void shouldFailWhenUnarchivingARoutineThatIsNotArchived() {
        assertThat(routine(RecurrencePeriod.WEEK, 3).unarchive(NOW).error())
            .isEqualTo(RoutineErrors.ROUTINE_NOT_ARCHIVED);
    }

    @Test
    void shouldRaiseDeletedEvent() {
        var routine = routine(RecurrencePeriod.WEEK, 3);

        routine.delete(NOW);

        assertThat(routine.pendingEvents()).containsExactly(new RoutineDeletedEvent(ROUTINE_ID, NOW));
    }

    @Test
    void shouldAllowLoggingOnAScheduledDay() {
        assertThat(daily(1, DayOfWeek.MONDAY).checkCanLogOn(MONDAY).isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenLoggingOnADayThatIsNotScheduled() {
        assertThat(daily(1, DayOfWeek.MONDAY).checkCanLogOn(TUESDAY).error())
            .isEqualTo(RoutineErrors.DAY_NOT_SCHEDULED);
    }

    @Test
    void shouldFailWhenLoggingOnAnArchivedRoutine() {
        assertThat(archived().checkCanLogOn(MONDAY).error()).isEqualTo(RoutineErrors.ROUTINE_IS_ARCHIVED);
    }

    @Test
    void shouldAllowLoggingOnDaysFarInThePast() {
        assertThat(routine(RecurrencePeriod.WEEK, 3).checkCanLogOn(LocalDate.of(2019, 1, 1)).isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenLoggingOnAMonthThatHasNoSuchDay() {
        var routine = monthlyOnDays(1, 31);

        assertThat(routine.checkCanLogOn(LocalDate.of(2026, 1, 31)).isSuccess()).isTrue();
        assertThat(routine.checkCanLogOn(LocalDate.of(2026, 2, 28)).error())
            .isEqualTo(RoutineErrors.DAY_NOT_SCHEDULED);
    }

    private static Routine archived() {
        var routine = routine(RecurrencePeriod.WEEK, 3);
        Result<Void> ignored = routine.archive(NOW);
        routine.clearEvents();

        return routine;
    }
}
