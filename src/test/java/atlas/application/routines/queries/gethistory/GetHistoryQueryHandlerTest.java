package atlas.application.routines.queries.gethistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.dto.HistoryDayDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.support.builders.RoutineFixtures;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetHistoryQueryHandlerTest {

    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);
    private static final LocalDate WED_11 = LocalDate.of(2026, 2, 11);
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetHistoryQueryHandler handler = new GetHistoryQueryHandler(routines);

    @Test
    void shouldReturnEveryDayOfTheRangeIncludingTheEmptyOnes() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(TUE_10, 1)));

        var result = handler.handle(new GetHistoryQuery(ID, MON_09, WED_11));

        assertThat(result.value())
            .extracting(HistoryDayDto::day, day -> day.logged().intValue(), HistoryDayDto::scheduled)
            .containsExactly(
                tuple(MON_09, 0, true),
                tuple(TUE_10, 1, true),
                tuple(WED_11, 0, true));
    }

    @Test
    void shouldMarkDaysThatAreNotScheduled() {
        when(routines.find(ID)).thenReturn(Optional.of(
            RoutineFixtures.daily(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of());

        var result = handler.handle(new GetHistoryQuery(ID, MON_09, WED_11));

        assertThat(result.value()).extracting(HistoryDayDto::scheduled).containsExactly(true, false, true);
    }

    @Test
    void shouldAddUpSeveralEntriesOfTheSameDay() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findEntries(eq(ID), any(), any()))
            .thenReturn(List.of(RoutineFixtures.entry(MON_09, 1), RoutineFixtures.entry(MON_09, 2)));

        var result = handler.handle(new GetHistoryQuery(ID, MON_09, MON_09));

        assertThat(result.value()).singleElement().extracting(day -> day.logged().intValue()).isEqualTo(3);
    }

    @Test
    void shouldReturnNothingWhenTheRangeIsInverted() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.daily(1, DayOfWeek.values())));

        assertThat(handler.handle(new GetHistoryQuery(ID, WED_11, MON_09)).value()).isEmpty();
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.find(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new GetHistoryQuery(ID, MON_09, WED_11)).error())
            .isEqualTo(RoutineErrors.notFound(ID));
    }
}
