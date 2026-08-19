package atlas.application.routines.commands.logprogress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineEntryIdGenerator;
import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.services.RoutineProgress;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogProgressCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-11T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final RoutineEntryRepository entries = mock(RoutineEntryRepository.class);
    private final RoutineEntryIdGenerator entryIds = mock(RoutineEntryIdGenerator.class);
    private final LogProgressCommandHandler handler = new LogProgressCommandHandler(
        unitOfWork, new RoutineProgress(), entryIds, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines, entries);
        when(entryIds.next()).thenReturn(RoutineEntryId.of(UUID.randomUUID()));
    }

    @Test
    void shouldCreateAnEntryAndReturnTheProgressOfTheWholePeriod() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));
        when(entries.find(ID, MON_09)).thenReturn(Optional.empty());
        when(entries.findInWindow(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(MON_09, 1)));

        var result = handler.handle(new LogProgressCommand(ID, MON_09, BigDecimal.ONE));

        assertThat(result.value().logged()).isEqualByComparingTo("1");
        assertThat(result.value().target()).isEqualByComparingTo("3");
        assertThat(result.value().met()).isFalse();
        assertThat(result.value().periodStart()).isEqualTo(MON_09);
        assertThat(result.value().periodEnd()).isEqualTo(MON_09.plusDays(7));
        verify(entries).create(any());
    }

    @Test
    void shouldAddOnTopOfTheSameDayInsteadOfCreatingAnother() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        var existing = RoutineFixtures.entry(MON_09, 1);
        when(routines.get(ID)).thenReturn(Optional.of(routine));
        when(entries.find(ID, MON_09)).thenReturn(Optional.of(existing));
        when(entries.findInWindow(eq(ID), any(), any())).thenReturn(List.of(existing));

        var result = handler.handle(new LogProgressCommand(ID, MON_09, BigDecimal.valueOf(2)));

        assertThat(existing.amount()).isEqualByComparingTo("3");
        assertThat(result.value().logged()).isEqualByComparingTo("3");
        assertThat(result.value().met()).isTrue();
        verify(entries).update(existing);
        verify(entries, never()).create(any());
    }

    @Test
    void shouldAccumulateEveryEntryOfThePeriod() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));
        when(entries.find(ID, TUE_10)).thenReturn(Optional.empty());
        when(entries.findInWindow(eq(ID), any(), any()))
            .thenReturn(List.of(RoutineFixtures.entry(MON_09, 2), RoutineFixtures.entry(TUE_10, 1)));

        var result = handler.handle(new LogProgressCommand(ID, TUE_10, BigDecimal.ONE));

        assertThat(result.value().logged()).isEqualByComparingTo("3");
        assertThat(result.value().met()).isTrue();
    }

    @Test
    void shouldFailWhenTheDayIsNotScheduled() {
        when(routines.get(ID)).thenReturn(Optional.of(
            RoutineFixtures.daily(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)));

        var result = handler.handle(new LogProgressCommand(ID, TUE_10, BigDecimal.ONE));

        assertThat(result.error()).isEqualTo(RoutineErrors.DAY_NOT_SCHEDULED);
        verify(entries, never()).create(any());
    }

    @Test
    void shouldFailOnAnArchivedRoutine() {
        when(routines.get(ID)).thenReturn(Optional.of(RoutineFixtures.archived(RecurrencePeriod.WEEK, 3)));

        var result = handler.handle(new LogProgressCommand(ID, MON_09, BigDecimal.ONE));

        assertThat(result.error()).isEqualTo(RoutineErrors.ROUTINE_IS_ARCHIVED);
        verify(entries, never()).create(any());
    }

    @Test
    void shouldFailWhenTheAmountIsNotPositive() {
        when(routines.get(ID)).thenReturn(Optional.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));
        when(entries.find(ID, MON_09)).thenReturn(Optional.empty());

        var result = handler.handle(new LogProgressCommand(ID, MON_09, BigDecimal.ZERO));

        assertThat(result.error()).isEqualTo(RoutineErrors.AMOUNT_MUST_BE_POSITIVE);
        verify(entries, never()).create(any());
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new LogProgressCommand(ID, MON_09, BigDecimal.ONE)).error())
            .isEqualTo(RoutineErrors.notFound(ID));
    }

    @Test
    void shouldAcceptDaysFarInThePast() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        var day = LocalDate.of(2019, 3, 4);
        when(routines.get(ID)).thenReturn(Optional.of(routine));
        when(entries.find(ID, day)).thenReturn(Optional.empty());
        when(entries.findInWindow(eq(ID), any(), any())).thenReturn(List.of());

        assertThat(handler.handle(new LogProgressCommand(ID, day, BigDecimal.ONE)).isSuccess()).isTrue();
    }
}
