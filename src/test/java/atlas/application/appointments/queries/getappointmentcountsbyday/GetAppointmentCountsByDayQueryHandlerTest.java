package atlas.application.appointments.queries.getappointmentcountsbyday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.dto.DailyAppointmentCountDto;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.domain.appointments.enums.CalendarPeriod;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAppointmentCountsByDayQueryHandlerTest {

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final GetAppointmentCountsByDayQueryHandler handler =
        new GetAppointmentCountsByDayQueryHandler(appointments);

    @BeforeEach
    void returnNoCountsByDefault() {
        when(appointments.countByDay(any(), anyBoolean())).thenReturn(List.of());
    }

    @Test
    void shouldReturnOneZeroBucketPerDayOfAShortMonth() {
        var result = handler.handle(new GetAppointmentCountsByDayQuery(YearMonth.of(2026, 2), false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).hasSize(28);
        assertThat(result.value()).extracting(DailyAppointmentCountDto::count).containsOnly(0L);
    }

    @Test
    void shouldReturnOneZeroBucketPerDayOfALongMonth() {
        var result = handler.handle(new GetAppointmentCountsByDayQuery(YearMonth.of(2026, 8), false));

        assertThat(result.value()).hasSize(31);
        assertThat(result.value().getFirst().day()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(result.value().getLast().day()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void shouldFillTheDaysTheReadModelReturnsAndZeroTheRest() {
        when(appointments.countByDay(any(), anyBoolean())).thenReturn(List.of(
            new DailyAppointmentCount(LocalDate.of(2026, 8, 17), 3)));

        var result = handler.handle(new GetAppointmentCountsByDayQuery(YearMonth.of(2026, 8), false));

        assertThat(result.value().get(16).count()).isEqualTo(3);
        assertThat(result.value().get(0).count()).isZero();
        assertThat(result.value().get(30).count()).isZero();
    }

    @Test
    void shouldAskTheReadModelForTheWholeMonthWindow() {
        handler.handle(new GetAppointmentCountsByDayQuery(YearMonth.of(2026, 8), true));

        verify(appointments).countByDay(CalendarPeriod.MONTH.windowFor(LocalDate.of(2026, 8, 1)), true);
    }
}
