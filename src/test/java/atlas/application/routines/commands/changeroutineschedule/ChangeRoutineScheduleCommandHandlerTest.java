package atlas.application.routines.commands.changeroutineschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineScheduleChangedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeRoutineScheduleCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final ChangeRoutineScheduleCommandHandler handler =
        new ChangeRoutineScheduleCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines);
    }

    @Test
    void shouldReplaceScheduleAndTargetAndRaiseItsEvent() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new ChangeRoutineScheduleCommand(
            ID, BigDecimal.valueOf(2), "L", RecurrencePeriod.DAY, Set.of(DayOfWeek.MONDAY), Set.of()));

        assertThat(result.value().period()).isEqualTo("DAY");
        assertThat(result.value().target()).isEqualByComparingTo("2");
        assertThat(result.value().unit()).isEqualTo("L");
        assertThat(routine.pendingEvents())
            .containsExactly(new RoutineScheduleChangedEvent(ID, routine.schedule(), NOW));
        verify(routines).update(routine);
    }

    @Test
    void shouldFailOnAnArchivedRoutine() {
        when(routines.get(ID)).thenReturn(Optional.of(RoutineFixtures.archived(RecurrencePeriod.WEEK, 3)));

        var result = handler.handle(new ChangeRoutineScheduleCommand(
            ID, BigDecimal.ONE, null, RecurrencePeriod.MONTH, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.ROUTINE_IS_ARCHIVED);
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new ChangeRoutineScheduleCommand(
            ID, BigDecimal.ONE, null, RecurrencePeriod.MONTH, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.notFound(ID));
    }

    @Test
    void shouldFailWhenTheScheduleIsIncoherent() {
        var result = handler.handle(new ChangeRoutineScheduleCommand(
            ID, BigDecimal.ONE, null, RecurrencePeriod.WEEK, Set.of(DayOfWeek.MONDAY), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.ACTIVE_DAYS_NOT_ALLOWED_FOR_THIS_PERIOD);
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheTargetIsNotPositive() {
        var result = handler.handle(new ChangeRoutineScheduleCommand(
            ID, BigDecimal.ZERO, null, RecurrencePeriod.MONTH, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.TARGET_MUST_BE_POSITIVE);
    }
}
