package atlas.application.appointments.queries.getappointmentcountsbymonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.dto.MonthlyAppointmentCountDto;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.domain.appointments.enums.CalendarPeriod;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAppointmentCountsByMonthQueryHandlerTest {

    private static final Year YEAR = Year.of(2026);

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final GetAppointmentCountsByMonthQueryHandler handler =
        new GetAppointmentCountsByMonthQueryHandler(appointments);

    @BeforeEach
    void returnNoCountsByDefault() {
        when(appointments.countByMonth(any(), anyBoolean())).thenReturn(List.of());
    }

    @Test
    void shouldReturnTwelveZeroBucketsWhenTheYearIsEmpty() {
        var result = handler.handle(new GetAppointmentCountsByMonthQuery(YEAR, false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).hasSize(12);
        assertThat(result.value()).extracting(MonthlyAppointmentCountDto::month)
            .containsExactly(
                YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3), YearMonth.of(2026, 4),
                YearMonth.of(2026, 5), YearMonth.of(2026, 6), YearMonth.of(2026, 7), YearMonth.of(2026, 8),
                YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12));
        assertThat(result.value()).extracting(MonthlyAppointmentCountDto::count).containsOnly(0L);
    }

    @Test
    void shouldFillTheMonthsTheReadModelReturnsAndZeroTheRest() {
        when(appointments.countByMonth(any(), anyBoolean())).thenReturn(List.of(
            new MonthlyAppointmentCount(YearMonth.of(2026, 3), 2),
            new MonthlyAppointmentCount(YearMonth.of(2026, 8), 5)));

        var result = handler.handle(new GetAppointmentCountsByMonthQuery(YEAR, false));

        assertThat(result.value()).hasSize(12);
        assertThat(result.value().get(2).count()).isEqualTo(2);
        assertThat(result.value().get(7).count()).isEqualTo(5);
        assertThat(result.value().get(0).count()).isZero();
    }

    @Test
    void shouldAskTheReadModelForTheWholeYearWindow() {
        handler.handle(new GetAppointmentCountsByMonthQuery(YEAR, true));

        verify(appointments).countByMonth(CalendarPeriod.YEAR.windowFor(LocalDate.of(2026, 1, 1)), true);
    }
}
