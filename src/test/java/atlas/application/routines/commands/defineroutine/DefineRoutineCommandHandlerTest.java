package atlas.application.routines.commands.defineroutine;

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
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.vos.Schedule;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefineRoutineCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineId.of(1);

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final DefineRoutineCommandHandler handler =
        new DefineRoutineCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines);
        when(routines.nextId()).thenReturn(ID);
    }

    @Test
    void shouldCreateTheRoutineAndRaiseItsEvent() {
        var result =
            handler.handle(command("Correr", BigDecimal.valueOf(3), RecurrencePeriod.WEEK, Set.of(), Set.of()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("R00000001");
        assertThat(result.value().name()).isEqualTo("Correr");
        verify(routines).create(any());
    }

    @Test
    void shouldRaiseTheDefinedEventOnTheAggregate() {
        var captured = new java.util.ArrayList<atlas.domain.routines.Routine>();
        org.mockito.Mockito.doAnswer(call -> captured.add(call.getArgument(0))).when(routines).create(any());

        handler.handle(command("Correr", BigDecimal.valueOf(3), RecurrencePeriod.WEEK, Set.of(), Set.of()));

        assertThat(captured).singleElement().satisfies(routine -> assertThat(routine.pendingEvents())
            .singleElement()
            .isEqualTo(new RoutineDefinedEvent(ID, routine.name(), NOW)));
    }

    @Test
    void shouldKeepTheDaysOfMonthOfAMonthlyRoutine() {
        var result = handler.handle(command(
            "Cierre de mes", BigDecimal.ONE, RecurrencePeriod.MONTH, Set.of(), Set.of(1, Schedule.LAST_DAY)));

        assertThat(result.value().daysOfMonth()).containsExactly(0, 1);
    }

    @Test
    void shouldFailWhenTheNameIsBlank() {
        var result = handler.handle(command("  ", BigDecimal.ONE, RecurrencePeriod.WEEK, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.NAME_REQUIRED);
        verify(routines, never()).create(any());
    }

    @Test
    void shouldFailWhenTheTargetIsNotPositive() {
        var result = handler.handle(command("Correr", BigDecimal.ZERO, RecurrencePeriod.WEEK, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.TARGET_MUST_BE_POSITIVE);
        verify(routines, never()).create(any());
    }

    @Test
    void shouldFailWhenTheScheduleIsIncoherent() {
        var result = handler.handle(command("Correr", BigDecimal.ONE, RecurrencePeriod.DAY, Set.of(), Set.of()));

        assertThat(result.error()).isEqualTo(RoutineErrors.ACTIVE_DAYS_REQUIRED_FOR_DAILY_ROUTINE);
        verify(routines, never()).create(any());
    }

    @Test
    void shouldFailWhenTheDescriptionIsTooLong() {
        var command = new DefineRoutineCommand(
            "Correr", "a".repeat(1001), BigDecimal.ONE, null, RecurrencePeriod.WEEK, Set.of(), Set.of());

        assertThat(handler.handle(command).error()).isEqualTo(RoutineErrors.DESCRIPTION_TOO_LONG);
    }

    @Test
    void shouldKeepTheUnitOfTheTarget() {
        var command = new DefineRoutineCommand(
            "Beber agua", null, new BigDecimal("1.5"), "L", RecurrencePeriod.DAY,
            Set.of(DayOfWeek.MONDAY), Set.of());

        var result = handler.handle(command);

        assertThat(result.value().unit()).isEqualTo("L");
        assertThat(result.value().target()).isEqualByComparingTo("1.5");
    }

    private static DefineRoutineCommand command(
        String name,
        BigDecimal target,
        RecurrencePeriod period,
        Set<DayOfWeek> activeDays,
        Set<Integer> daysOfMonth) {
        return new DefineRoutineCommand(name, null, target, null, period, activeDays, daysOfMonth);
    }
}
