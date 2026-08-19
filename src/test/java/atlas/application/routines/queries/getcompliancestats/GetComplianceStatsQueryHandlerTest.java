package atlas.application.routines.queries.getcompliancestats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.dto.ComplianceStatsDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.services.RoutineProgress;
import atlas.support.builders.RoutineFixtures;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetComplianceStatsQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 11);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetComplianceStatsQueryHandler handler =
        new GetComplianceStatsQueryHandler(routines, new RoutineProgress(), CLOCK);

    @Test
    void shouldCountOnlyClosedPeriods() {
        when(routines.findAll(false)).thenReturn(List.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(MON_09, 1)));

        var result = handler.handle(new GetComplianceStatsQuery(MON_09, TODAY, false));

        assertThat(result.value()).singleElement().satisfies(stats -> {
            assertThat(stats.periodsClosed()).isEqualTo(2);
            assertThat(stats.periodsMet()).isEqualTo(1);
            assertThat(stats.ratio()).isEqualTo(0.5d);
        });
    }

    @Test
    void shouldNotCountPeriodsWithoutOccurrence() {
        when(routines.findAll(false)).thenReturn(List.of(
            RoutineFixtures.daily(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(MON_09, 1)));

        var result = handler.handle(new GetComplianceStatsQuery(MON_09, TODAY, false));

        assertThat(result.value()).singleElement().satisfies(stats -> {
            assertThat(stats.periodsClosed()).isEqualTo(1);
            assertThat(stats.periodsMet()).isEqualTo(1);
            assertThat(stats.ratio()).isEqualTo(1d);
        });
    }

    @Test
    void shouldReportZeroRatioWhenNothingClosedYet() {
        when(routines.findAll(false)).thenReturn(List.of(RoutineFixtures.daily(1, DayOfWeek.values())));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(TODAY, 1)));

        var result = handler.handle(new GetComplianceStatsQuery(TODAY, TODAY, false));

        assertThat(result.value())
            .extracting(ComplianceStatsDto::periodsClosed, ComplianceStatsDto::ratio)
            .containsExactly(tuple(0, 0d));
    }

    @Test
    void shouldReturnNothingWhenTheRangeIsInverted() {
        assertThat(handler.handle(new GetComplianceStatsQuery(TODAY, MON_09, false)).value()).isEmpty();
    }
}
