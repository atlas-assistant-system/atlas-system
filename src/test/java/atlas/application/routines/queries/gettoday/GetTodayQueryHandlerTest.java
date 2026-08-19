package atlas.application.routines.queries.gettoday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.dto.TodayRoutineDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.services.RoutineProgress;
import atlas.domain.routines.vos.Schedule;
import atlas.support.builders.RoutineFixtures;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetTodayQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 11);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetTodayQueryHandler handler = new GetTodayQueryHandler(routines, new RoutineProgress(), CLOCK);

    @Test
    void shouldShowTheProgressOfTheWholePeriodNotOfTheDay() {
        when(routines.findAll(false)).thenReturn(List.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));
        when(routines.findEntries(anyList(), any(), any()))
            .thenReturn(List.of(RoutineFixtures.entry(MON_09, 1), RoutineFixtures.entry(TUE_10, 1)));

        var result = handler.handle(new GetTodayQuery());

        assertThat(result.value()).singleElement().satisfies(today -> {
            assertThat(today.progress().logged()).isEqualByComparingTo("2");
            assertThat(today.progress().target()).isEqualByComparingTo("3");
            assertThat(today.progress().met()).isFalse();
            assertThat(today.progress().closed()).isFalse();
            assertThat(today.progress().failed()).isFalse();
        });
    }

    @Test
    void shouldHideRoutinesThatDoNotOccurToday() {
        when(routines.findAll(false))
            .thenReturn(List.of(RoutineFixtures.daily(1, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)));

        assertThat(handler.handle(new GetTodayQuery()).value()).isEmpty();
    }

    @Test
    void shouldNotAskForEntriesWhenNothingIsDue() {
        when(routines.findAll(false)).thenReturn(List.of());

        assertThat(handler.handle(new GetTodayQuery()).value()).isEmpty();
        org.mockito.Mockito.verify(routines, org.mockito.Mockito.never()).findEntries(anyList(), any(), any());
    }

    @Test
    void shouldNeverAskForArchivedRoutines() {
        when(routines.findAll(anyBoolean())).thenReturn(List.of());

        handler.handle(new GetTodayQuery());

        org.mockito.Mockito.verify(routines).findAll(false);
    }

    @Test
    void shouldNotMixTheEntriesOfOneRoutineIntoAnother() {
        var weekly = Schedule.over(RecurrencePeriod.WEEK).value();
        var marked = RoutineFixtures.routine(RoutineId.of(1), "Con registros", 3, weekly);
        var empty = RoutineFixtures.routine(RoutineId.of(2), "Sin registros", 3, weekly);
        when(routines.findAll(false)).thenReturn(List.of(marked, empty));
        when(routines.findEntries(anyList(), any(), any()))
            .thenReturn(List.of(RoutineFixtures.entry(RoutineId.of(1), MON_09, 1)));

        var result = handler.handle(new GetTodayQuery());

        assertThat(result.value())
            .extracting(today -> today.routine().name(), today -> today.progress().logged().intValue())
            .containsExactly(tuple("Con registros", 1), tuple("Sin registros", 0));
    }

    @Test
    void shouldCarryBothTheRoutineAndItsProgress() {
        when(routines.findAll(false)).thenReturn(List.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));
        when(routines.findEntries(anyList(), any(), any())).thenReturn(List.of());

        var result = handler.handle(new GetTodayQuery());

        assertThat(result.value()).singleElement().extracting(TodayRoutineDto::routine).isNotNull();
        assertThat(result.value().getFirst().progress().routineId()).isEqualTo(RoutineFixtures.ROUTINE_ID.toString());
    }
}
