package atlas.application.routines.queries.getperiodprogress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.services.RoutineProgress;
import atlas.support.builders.RoutineFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPeriodProgressQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 11);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate LAST_WEEK = LocalDate.of(2026, 2, 3);
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetPeriodProgressQueryHandler handler =
        new GetPeriodProgressQueryHandler(routines, new RoutineProgress(), CLOCK);

    @Test
    void shouldReportAPastPeriodAsClosedAndFailed() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(LAST_WEEK, 1)));

        var result = handler.handle(new GetPeriodProgressQuery(ID, LAST_WEEK));

        assertThat(result.value().logged()).isEqualByComparingTo("1");
        assertThat(result.value().closed()).isTrue();
        assertThat(result.value().failed()).isTrue();
    }

    @Test
    void shouldNotReportTheOpenPeriodAsFailed() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));
        when(routines.findEntries(eq(ID), any(), any())).thenReturn(List.of(RoutineFixtures.entry(MON_09, 1)));

        var result = handler.handle(new GetPeriodProgressQuery(ID, TODAY));

        assertThat(result.value().closed()).isFalse();
        assertThat(result.value().failed()).isFalse();
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.find(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new GetPeriodProgressQuery(ID, TODAY)).error())
            .isEqualTo(RoutineErrors.notFound(ID));
    }
}
