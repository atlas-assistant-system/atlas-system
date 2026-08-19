package atlas.application.routines.queries.getstreak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.services.RoutineProgress;
import atlas.support.builders.RoutineFixtures;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetStreakQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 11);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetStreakQueryHandler handler =
        new GetStreakQueryHandler(routines, new RoutineProgress(), CLOCK);

    @Test
    void shouldReportCurrentAndBestStreak() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findAllEntries(ID)).thenReturn(List.of(
            RoutineFixtures.entry(MON_09, 1),
            RoutineFixtures.entry(TUE_10, 1),
            RoutineFixtures.entry(TODAY, 1)));

        var result = handler.handle(new GetStreakQuery(ID));

        assertThat(result.value().current()).isEqualTo(3);
        assertThat(result.value().best()).isEqualTo(3);
        assertThat(result.value().routineId()).isEqualTo(ID.toString());
    }

    @Test
    void shouldBeZeroWithoutHistory() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findAllEntries(ID)).thenReturn(List.of());

        assertThat(handler.handle(new GetStreakQuery(ID)).value().current()).isZero();
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.find(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new GetStreakQuery(ID)).error()).isEqualTo(RoutineErrors.notFound(ID));
    }
}
